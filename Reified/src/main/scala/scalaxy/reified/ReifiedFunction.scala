package scalaxy.reified

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.Expr
  
class ReifiedFunction[A, B](
  f: A => B,
  taggedExpr: Expr[A => B],
  captures: Seq[AnyRef])
    extends ReifiedValue[A => B](f, taggedExpr, captures)
    with Function1[A, B] {
    
  override def apply(a: A): B = f(a)
  
  override def compose[C](g: C => A): ReifiedFunction[C, B] = g match {
    case gg: ReifiedFunction[_, _] =>
      compose(gg).asInstanceOf[ReifiedFunction[C, B]]
    case _ =>
      sys.error("Cannot compose a ReifiedFunction with a simple Function")
  }
  
  def compose[C](g: ReifiedFunction[C, A]): ReifiedFunction[C, B] = {
    ReifiedFunction.compose(g, this)
  }
  
  override def andThen[C](g: B => C): ReifiedFunction[A, C] = g match {
    case gg: ReifiedFunction[_, _] =>
      andThen(gg).asInstanceOf[ReifiedFunction[A, C]]
    case _ =>
      sys.error("Cannot compose a ReifiedFunction with a simple Function")
  }
  
  def andThen[C](g: ReifiedFunction[B, C]): ReifiedFunction[A, C] = {
    ReifiedFunction.compose(this, g)
  }
}

object ReifiedFunction {
  def compose[A, B, C](ab: ReifiedFunction[A, B], bc: ReifiedFunction[B, C]): ReifiedFunction[A, C] = {
    composeValues[A => C](Seq(ab, bc))({ 
      case Seq(abTaggedExpr: Expr[A => B], bcTaggedExpr: Expr[B => C]) =>
        (
          bc.value.compose(ab.value),
          universe.reify({
            (a: A) => {
              // TODO: treat `val x = function` as a def in ScalaCL
              val ab = abTaggedExpr.splice
              val bc = bcTaggedExpr.splice
              bc(ab(a))
            }
          })
        )
    }).asInstanceOf[ReifiedFunction[A, C]]
  }
}
