package scalaxy

import scala.language.experimental.macros

import scala.reflect._
import scala.reflect.macros.Context
import scala.reflect.runtime
import scala.tools.reflect.ToolBox

package object reified
{
  def reify[A, B](f: A => B): ReifiedFunction[A, B] = macro reified.impl.reifyFunction[A, B]
  
  def reify[A](v: A): ReifiedValue[A] = macro reified.impl.reifyValue[A]
}

package reified 
{
  object impl 
  {
    def capture[T](index: Int): T = ???
    
    def reifyFunction[A : c.WeakTypeTag, B : c.WeakTypeTag](c: Context)(f: c.Expr[A => B]): c.Expr[ReifiedFunction[A, B]] = {
      val (tree, capturesExpr) = transformReifiedRefs(c)(c.typeCheck(f.tree))
      val expr = c.Expr[runtime.universe.Expr[A => B]](
        c.reifyTree(
          c.universe.treeBuild.mkRuntimeUniverseRef,
          c.universe.EmptyTree,
          tree
        )
      )
      c.universe.reify({
        new ReifiedFunction[A, B](f.splice, expr.splice, capturesExpr.splice)
      })
    }
    
    def reifyValue[A : c.WeakTypeTag](c: Context)(v: c.Expr[A]): c.Expr[ReifiedValue[A]] = {
      val (tree, capturesExpr) = transformReifiedRefs(c)(c.typeCheck(v.tree))
      val expr = c.Expr[runtime.universe.Expr[A]](
        c.reifyTree(
          c.universe.treeBuild.mkRuntimeUniverseRef,
          c.universe.EmptyTree,
          tree
        )
      )
      c.universe.reify({
        new ReifiedValue[A](v.splice, expr.splice, capturesExpr.splice)
      })
    }
    
    def transformReifiedRefs(c: Context)(tree: c.universe.Tree): (c.universe.Tree, c.Expr[Seq[AnyRef]]) = {
      import c.universe._
      import definitions._
      
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
      val capturedTerms = collection.mutable.ArrayBuffer[Tree]()
      val capturedSymbols = collection.mutable.HashMap[TermSymbol, Int]()
      
      //println(s"localDefSyms = $localDefSyms")
      val transformer = new Transformer {
        override def transform(t: Tree): Tree = {
          if (t.symbol != null && !isDefLike(t)) {
            val sym = t.symbol
            //println(s"transform($t { tpe = ${t.tpe}, symbol = ${sym}, isFreeTerm = ${sym.isFreeTerm}, owner = ${sym.owner}, isMethod = ${sym.isMethod}, isLocal = ${sym.isLocal} })")
            if (sym.isTerm && !localDefSyms.contains(sym)) {
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
                      capturedTerms += Ident(tsym)//t
                      
                      lastCaptureIndex
                  }
                )
                
                val tpe = t.tpe.normalize.widen
                //println(s"tpe = $tpe: ${tpe.getClass.getName}")
                //println(s"symbol.tpe = ${sym.typeSignature}
                // Abuse reify to get correct reference to `capture`.
                val Apply(TypeApply(f, List(_)), args) = {
                  reify(reified.impl.capture[Int](captureIndexExpr.splice)).tree
                }
                c.typeCheck(
                  Apply(TypeApply(f, List(TypeTree(tpe))), args),
                  tpe
                )
              } else if (tsym.isMethod) {
                super.transform(t)
              } else {
                c.error(t.pos, s"Capture of this expression is not supported (symbol = $tsym)")
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
      
      //println(s"Transforming $tree")
      val transformed = transformer.transform(tree)
      //println(s"Result: $transformed")
      
      val arrExpr = {
        // Abuse reify to get correct seq constructor
        val Apply(seqConstructor, _) = reify(Seq[AnyRef]()).tree
        c.Expr[Seq[AnyRef]](
          c.typeCheck(
            Apply(
              seqConstructor,
              capturedTerms.map(term => {
                val termExpr = c.Expr[Any](term)
                reify(termExpr.splice.asInstanceOf[AnyRef]).tree
              }).toList),
            c.typeOf[Seq[AnyRef]]))
      }
      (transformed, arrExpr)
    }
  }
  
  class ReifiedValue[A](
      val value: A,
      private[reified] val rawExpr: runtime.universe.Expr[A],
      val captures: Seq[AnyRef]) {
    val expr = {
      // TODO: replace captures
      rawExpr
    }
  }
  
  class ReifiedFunction[A, B](
    f: A => B,
    rawExpr: runtime.universe.Expr[A => B],
    captures: Seq[AnyRef])
      extends ReifiedValue[A => B](f, rawExpr, captures)
      with Function1[A, B] {
      
    import runtime.universe._
    
    def apply(a: A): B = f(a)
    
    override def compose[C](g: C => A): C => B = g match {
      case gg: ReifiedFunction[_, _] =>
        compose(gg)
      case _ =>
        sys.error("Cannot compose a ReifiedFunction with a simple Function")
    }
    
    def compose[C](g: ReifiedFunction[C, A]): C => B = {
      new ReifiedFunction[C, B](
        f.compose(g),
        reify({
          (c: C) => {
            // TODO: treat `val x = function` as a def in ScalaCL
            val ff = expr.splice
            val gg = g.expr.splice
            ff(gg(c))
          }
        }),
        // TODO: offset captures in g.expr by captures.size
        captures ++ g.captures
      )
    }
  }
}
