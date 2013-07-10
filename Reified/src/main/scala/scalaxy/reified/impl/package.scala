package scalaxy.reified

import scala.language.experimental.macros

import scala.reflect._
import scala.reflect.macros.Context
import scala.reflect.runtime.universe

package object impl {
  
  private def runtimeExpr[A](c: Context)(tree: c.universe.Tree): c.Expr[universe.Expr[A]] = {
    c.Expr[universe.Expr[A]](
      c.reifyTree(
        c.universe.treeBuild.mkRuntimeUniverseRef,
        c.universe.EmptyTree,
        tree
      )
    )
  }
  def reifyFunction[A : c.WeakTypeTag, B : c.WeakTypeTag](c: Context)(f: c.Expr[A => B]): c.Expr[ReifiedFunction[A, B]] = {
    val (expr, capturesExpr) = transformReifiedRefs(c)(f)
    c.universe.reify({
      new ReifiedFunction[A, B](
        f.splice, 
        Utils.typeCheck(expr.splice), 
        capturesExpr.splice
      )
    })
  }
  
  def reifyValue[A : c.WeakTypeTag](c: Context)(v: c.Expr[A]): c.Expr[ReifiedValue[A]] = {
    val (expr, capturesExpr) = transformReifiedRefs(c)(v)
    c.universe.reify({
      // Use factory method instead of constructor: in case the runtime
      // type of the value is a function, it will call ReifiedFunction's
      // constructor instead.
      ReifiedValue[A](
        v.splice, 
        Utils.typeCheck(expr.splice), 
        capturesExpr.splice
      )
    })
  }
  
  /** Detect captured references, replace them by capture tags and
   *  return their ordered list along with the resulting tree.
   */
  private def transformReifiedRefs[A](c: Context)(expr: c.Expr[A]): (c.Expr[universe.Expr[A]], c.Expr[Seq[(AnyRef, universe.Type)]]) = {
    import c.universe._
    import definitions._
    
    val tree = c.typeCheck(expr.tree)
    
    val localDefSyms = collection.mutable.HashSet[Symbol]()
    def isDefLike(t: Tree) = t match {
      case Function(_, _) => true
      case _ if t.isDef => true
      case _ => false
    }
    (new Traverser {
      override def traverse(t: Tree) {
        super.traverse(t)
        if (t.symbol != null && isDefLike(t)) {
          localDefSyms += t.symbol
        }
      }
    }).traverse(tree)
    
    var lastCaptureIndex = -1
    val capturedTerms = collection.mutable.ArrayBuffer[(Tree, Type)]()
    val capturedSymbols = collection.mutable.HashMap[TermSymbol, Int]()
    
    val transformer = new Transformer {
      override def transform(t: Tree): Tree = {
        if (t.tpe != null) {
          def checkType(tpe: Type) {
            if (tpe != NoType) {
              val sym = tpe.typeSymbol
              if (sym != NoSymbol) {
                val tsym = sym.asType
                if (!localDefSyms.contains(tsym)) {
                  if (tsym.isAbstractType || tsym.isExistential || tsym.isAliasType || sym.isFreeType) {
                    c.error(t.pos, "Cannot capture type " + tpe)
                  }
                }
              }
            }
          }
          checkType(t.tpe)
          t.tpe.foreach(checkType(_))
        }
        if (t.symbol != null && !isDefLike(t)) {
          val sym = t.symbol
          // TODO: fine-tune capture constraints.
          //println(s"transform($t { tpe = ${t.tpe}, symbol = ${sym}, isFreeTerm = ${sym.isFreeTerm}, owner = ${sym.owner}, isMethod = ${sym.isMethod}, isLocal = ${sym.isLocal} })")
          if (sym.isType && !localDefSyms.contains(sym)) {
            val tsym = sym.asType
            if (tsym.isAbstractType || tsym.isExistential || tsym.isAliasType) {
              c.error(t.pos, "Cannot capture this type")
              t
            } else {
              super.transform(t)
            }
          } else if (sym.isTerm && !localDefSyms.contains(sym)) {
            val tsym = sym.asTerm
            if (tsym.isVar) {
              c.error(t.pos, "Cannot capture a var")
              t
            } else if (tsym.isLazy) {
              c.error(t.pos, "Cannot capture lazy vals")
              t
            } else if (tsym.isVal || tsym.isAccessor) {
              val captureIndexExpr = c.literal(
                capturedSymbols.get(tsym) match {
                  case Some(i) =>
                    i
                  case None =>
                    lastCaptureIndex += 1
                    capturedSymbols += tsym -> lastCaptureIndex
                    capturedTerms += Ident(tsym) -> t.tpe
                    
                    lastCaptureIndex
                }
              )
              
              val tpe = t.tpe.normalize.widen
              // Abuse reify to get correct reference to `capture`.
              val Apply(TypeApply(f, List(_)), _) = {
                reify(scalaxy.reified.impl.CaptureTag[Int](10, 1)).tree
              }
              c.typeCheck(
                Apply(
                  TypeApply(
                    f,
                    List(TypeTree(tpe))),
                  List(t, captureIndexExpr.tree)),
                tpe)
            } else if (tsym.isMethod) {
              super.transform(t)
            } else {
              c.error(t.pos, s"Cannot capture this type of expression (symbol = $tsym)")
              t
            } 
          } else {
            super.transform(t)
          }
        } else {
          super.transform(t)
        }
      }
    }
    
    val transformedExpr = runtimeExpr[A](c)(transformer.transform(tree))
    val capturesArrayExpr = {
      // Abuse reify to get correct seq constructor
      val Apply(seqConstructor, _) = reify(Seq[(AnyRef, universe.Type)]()).tree
      c.Expr[Seq[(AnyRef, universe.Type)]](
        c.typeCheck(
          Apply(
            seqConstructor,
            capturedTerms.map({
              case (term, tpe) =>
                val termExpr = c.Expr[Any](term)
                val typeExpr = c.Expr[universe.TypeTag[_]](
                  c.reifyType(
                    treeBuild.mkRuntimeUniverseRef,
                    Select(
                      treeBuild.mkRuntimeUniverseRef,
                      newTermName("rootMirror")
                    ),
                    tpe.normalize.widen
                  )
                )
                //println("REIFIED TYPE of " + tpe + " is " + typeExpr)
                reify({
                  (
                    termExpr.splice.asInstanceOf[AnyRef],
                    typeExpr.splice.tpe
                  )
                }).tree
            }).toList),
          c.typeOf[Seq[(AnyRef, universe.Type)]]))
    }
    (transformedExpr, capturesArrayExpr)
  }
}
