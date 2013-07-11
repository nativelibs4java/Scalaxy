package scalaxy.reified

import scalaxy.reified.impl.{ Reification, HasReification }

class ReifiedValue[A] private[reified] (
  val value: A,
  val reification: Reification[A])
    extends HasReification[A] {
  
  def expr(conversion: CaptureConversions.Conversion = CaptureConversions.DEFAULT) = {
    reification.expr(conversion)
  }
        
  override def toString = {
    if (value == null)
      "null"
    else
      value.toString
  }
}

object ReifiedValue {
  implicit def reifiedValue2Value[A](r: ReifiedValue[A]): A = r.value

  def apply[A](value: A, reification: Reification[A]): ReifiedValue[A] = {
    // Do not check that reification.rawExpr is a Trees#Function, because it might be
    // a Block with declarations of captured values, ending with a Function.
    if (value.isInstanceOf[Function1[_, _]]) {
      new ReifiedFunction[Any, Any](
        value.asInstanceOf[Function[Any, Any]], 
        reification.asInstanceOf[Reification[Any => Any]]
      ).asInstanceOf[ReifiedValue[A]]
    } else {
      new ReifiedValue[A](value, reification)
    }
  }
}

