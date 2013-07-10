package scalaxy.reified

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.Expr
  
class ReifiedFunction[A, B](
  f: A => B,
  taggedExpr: Expr[A => B],
  captures: Seq[AnyRef])
    extends ReifiedValue[A => B](f, taggedExpr, captures)
    with Function1[A, B] {
    
  def apply(a: A): B = f(a)
  
  override def compose[C](g: C => A): ReifiedFunction[C, B] = g match {
    case gg: ReifiedFunction[_, _] =>
      compose(gg)
    case _ =>
      sys.error("Cannot compose a ReifiedFunction with a simple Function")
  }
  
  def compose[C](g: ReifiedFunction[C, A]): ReifiedFunction[C, B] = {
    composeValues[C => B](Seq(this, g))({ 
      case Seq(fTaggedExpr: Expr[A => B], gTaggedExpr: Expr[C => A]) =>
        (
          f.compose(g),
          universe.reify({
            (c: C) => {
              // TODO: treat `val x = function` as a def in ScalaCL
              val ff = fTaggedExpr.splice
              val gg = gTaggedExpr.splice
              ff(gg(c))
            }
          })
        )
    }).asInstanceOf[ReifiedFunction[C, B]]
  }
}
