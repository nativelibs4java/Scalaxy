package scalaxy.reified

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.Expr
import scala.reflect.ClassTag

import scalaxy.reified.impl.composeValues
import scalaxy.debug._

class ReifiedFunction1[A, B](
  val value: ReifiedValue[A => B])
    extends Function1[A, B] {

  assert(value != null)

  override def apply(a: A): B = value.value(a)

  private def cast[A1, B1](f: A1 => B1) = f match {
    case r: ReifiedFunction1[A1, B1] =>
      r
    case _ =>
      sys.error("Expected a ReifiedFunction1, got " + f)
  }

  override def compose[C](g: C => A): ReifiedFunction1[C, B] = {
    val r = cast(g)
    //val f = this
    //reify((c: C) => f(g(c)))
    ReifiedFunction1.compose(r, this)
  }

  override def andThen[C](g: B => C): ReifiedFunction1[A, C] = {
    val r = cast(g)
    //val f = this
    //reify((a: A) => g(f(a)))
    ReifiedFunction1.compose(this, r)
  }

  override def toString = value.toString
}

object ReifiedFunction1 {
  def compose[A, B, C](ab: ReifiedFunction1[A, B], bc: ReifiedFunction1[B, C]): ReifiedFunction1[A, C] = {
    //reify((a: A) => bc(ab(a)))
    composeValues[A => C](Seq(ab.value, bc.value))({
      case Seq(abTaggedExpr: Expr[A => B], bcTaggedExpr: Expr[B => C]) =>
        (
          bc.value.compose(ab.value),
          universe.reify({
            (a: A) =>
              {
                // TODO: treat `val x = function` as a def in ScalaCL
                val ab = abTaggedExpr.splice
                val bc = bcTaggedExpr.splice
                bc(ab(a))
              }
          })
        )
    }) //.asInstanceOf[ReifiedFunction1[A, C]]
  }
}
