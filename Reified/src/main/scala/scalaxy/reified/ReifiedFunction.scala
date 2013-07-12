package scalaxy.reified

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.Expr
import scala.reflect.runtime.universe.TypeTag
import scala.reflect.ClassTag

import scalaxy.reified.impl.composeValues
import scalaxy.debug._

class ReifiedFunction1[A: TypeTag, B: TypeTag](
  val value: ReifiedValue[A => B])
    extends HasReifiedValue[A => B] {

  assert(value != null)

  override def reifiedValue = value

  def apply(a: A): B = value.value(a)

  private def cast[A1, B1](f: A1 => B1) = f match {
    case r: ReifiedFunction1[A1, B1] =>
      r
    case _ =>
      sys.error("Expected a ReifiedFunction1, got " + f)
  }

  //def compose[C: TypeTag](g: C => A): ReifiedFunction1[C, B] = {
  //  val r = cast(g)
  def compose[C: TypeTag](g: ReifiedFunction1[C, A]): ReifiedFunction1[C, B] = {
    val f = this
    //implicit val C = TypeTag.Any.asInstanceOf[TypeTag[C]]
    //val tt = universe.typeTag[C]
    base.reify((c: C) => f(g(c)))
    //ReifiedFunction1.compose(r, this)
  }

  //def andThen[C: TypeTag](g: B => C): ReifiedFunction1[A, C] = {
  //  val r = cast(g)
  def andThen[C: TypeTag](g: ReifiedFunction1[B, C]): ReifiedFunction1[A, C] = {
    val f = this
    //implicit val C = TypeTag.Any.asInstanceOf[TypeTag[C]]
    base.reify((a: A) => g(f(a)))
    //ReifiedFunction1.compose(this, r)
  }

  override def toString = s"${getClass.getSimpleName}($value)"
}
/*
object ReifiedFunction1 {
  def compose[A: TypeTag, B: TypeTag, C: TypeTag](ab: ReifiedFunction1[A, B], bc: ReifiedFunction1[B, C]): ReifiedFunction1[A, C] = {
    //reify((a: A) => bc(ab(a)))
    composeValues[A => C](Seq(ab.value, bc.value))({
      case Seq(abTaggedExpr: Expr[A => B], bcTaggedExpr: Expr[B => C]) =>
        (
          bc.value.value.compose(ab.value.value),
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
    })
  }
}
*/
