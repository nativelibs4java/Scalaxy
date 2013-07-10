package scalaxy.reified

import scala.reflect.runtime.universe
  
class ReifiedFunction[A, B](
  f: A => B,
  rawExpr: universe.Expr[A => B],
  captures: Seq[AnyRef])
    extends ReifiedValue[A => B](f, rawExpr, captures)
    with Function1[A, B] {
    
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
      universe.reify({
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
