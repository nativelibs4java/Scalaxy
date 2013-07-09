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
  
  private[reified] def capture(index: Int) = ???
}

package reified 
{
  object impl 
  {
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
    
    def transformReifiedRefs(c: Context)(tree: c.universe.Tree): (c.universe.Tree, c.Expr[Array[AnyRef]]) = {
      import c.universe._
      import definitions._
      
      val transformer = new Transformer {
        override def transform(t: Tree): Tree = {
          if (t.symbol != null) {
            val sym = t.symbol
            println(s"transform($t { symbol = ${sym}, isFreeTerm = ${sym.isFreeTerm}, owner = ${sym.owner}, isMethod = ${sym.isMethod}, isLocal = ${sym.isLocal} })")
            if (sym.isTerm && !t.isDef) {
              val tsym = sym.asTerm
              if (tsym.isVar) {
                c.error(t.pos, "Cannot capture a var")
              //} else if (!tsym.isStable) {
              //  c.error(t.pos, "This reference is not stable, cannot capture it.")
              } else if (tsym.isLazy) {
                c.error(t.pos, "Cannot capture lazy vals")
              } else if (tsym.isMethod && !tsym.isGetter && !tsym.isAccessor) {
                // TODO: Do nothing... if supported method 
              } else {
                t.tpe match {
                  case tr @ TypeRef(_, _, List(vtpe)) 
                  if tr <:< typeOf[ReifiedValue[_]] =>
                    c.error(t.pos, "TODO: handle capture of reified terms")
                  case _ =>
                    c.error(t.pos, "Capture of this expression is not supported")
                }
              }
            }
          }
          super.transform(t)
        }
      }
      
      println(s"Transforming $tree")
      val res = transformer.transform(tree)
      println(s"Result: $res")
      (res, reify(Array[AnyRef]()))
    }
  }
  
  class ReifiedValue[A](
      val value: A,
      private[reified] val rawExpr: runtime.universe.Expr[A],
      private[reified] val captures: Array[AnyRef]) {
    val expr = {
      // TODO: replace captures
      rawExpr
    }
  }
  
  class ReifiedFunction[A, B](
    f: A => B,
    rawExpr: runtime.universe.Expr[A => B],
    captures: Array[AnyRef])
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
