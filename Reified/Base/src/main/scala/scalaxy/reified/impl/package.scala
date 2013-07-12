package scalaxy.reified

import scala.language.experimental.macros

import scala.reflect._
import scala.reflect.macros.Context
import scala.reflect.runtime.universe

import scalaxy.reified.base.ReifiedValue
import scalaxy.reified.impl.Utils._

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

  def reifyImpl[A: c.WeakTypeTag](c: Context)(v: c.Expr[A]): c.Expr[ReifiedValue[A]] = {

    import c.universe._

    val (expr, capturesExpr) = transformReifiedRefs(c)(v)
    c.universe.reify({
      new ReifiedValue[A](
        v.splice,
        Utils.typeCheck(expr.splice),
        capturesExpr.splice)
    })
  }

  /**
   * Detect captured references, replace them by capture tags and
   *  return their ordered list along with the resulting tree.
   */
  private def transformReifiedRefs[A](c: Context)(expr: c.Expr[A]): (c.Expr[universe.Expr[A]], c.Expr[Seq[(AnyRef, universe.Type)]]) = {
    //c.Expr[Reification[A]] = {
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
    val capturedTypeTags = collection.mutable.Set[String]()

    val transformer = new Transformer {
      override def transform(t: Tree): Tree = {
        // TODO check which types can be captured
        if (t.tpe != null && t.tpe != NoType && t.symbol != null) { // && t.symbol.isTerm) {
          def visitType(tpe: Type) {
            val sym = tpe.typeSymbol
            if (sym != NoSymbol) {
              val tsym = sym.asType
              if (tsym.isParameter) {
                val name = tsym.name.toString
                if (!capturedTypeTags.contains(name)) {
                  capturedTypeTags += name
                  val inferredTypeTag = {
                    c.inferImplicitValue(for (t <- typeOf[universe.TypeTag[Int]]) yield {
                      if (t == typeOf[Int])
                        tpe
                      else
                        t
                    })
                  }
                  if (inferredTypeTag == EmptyTree) {
                    c.error(t.pos, "Failed to find evidence for type variable " + name)
                    //println("Failed to find evidence for type variable " + name)
                  }
                }
              }
            }
          }
          val tpe = t.tpe.normalize.widen
          visitType(tpe)
          //tpe.foreach(visitType(_))
        }
        val sym = t.symbol
        if (sym != null && !isDefLike(t) && sym.isTerm && !localDefSyms.contains(sym)) {
          val tsym = sym.asTerm
          if (tsym.isVar) {
            c.error(t.pos, "Cannot capture this var: " + tsym)
            t
          } else if (tsym.isLazy && tsym.fullName != "scala.reflect.runtime.universe") {
            c.error(t.pos, "Cannot capture this lazy val: " + tsym)
            t
          } else if (tsym.isMethod || tsym.isModule && tsym.isStable) {
            super.transform(t)
          } else if (tsym.isVal || tsym.isAccessor) {
            val captureIndexExpr = c.literal(
              capturedSymbols.get(tsym) match {
                case Some(i) =>
                  i
                case None =>
                  lastCaptureIndex += 1
                  capturedSymbols += tsym -> lastCaptureIndex
                  capturedTerms += Ident(tsym) -> t.tpe
                  //println("Capture: " + t + " (" + tsym + ")")

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
          } else {
            c.error(t.pos, s"Cannot capture this type of expression (symbol = $tsym): " + tsym)
            t
          }
        } else {
          super.transform(t)
        }
      }
    }

    //println(s"Transforming $tree")
    val transformed = transformer.transform(tree)
    //println(s"Transformed to $transformed")
    val transformedExpr = runtimeExpr[A](c)(transformed)
    val capturesArrayExpr = {
      // Abuse reify to get correct seq constructor.
      val Apply(seqConstructor, _) = reify(Seq[(AnyRef, universe.Type)]()).tree
      // Build the list of captured terms (with their types).
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
