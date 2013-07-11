package scalaxy.reified

import scalaxy.reified.CaptureConversions
import scalaxy.reified.CaptureConversions.Conversion
import scalaxy.reified.impl.Reification

class ReifiedValue[A] private[reified] (
    val value: A,
    private[reified] val reification: Reification[A]) {
  
  def expr(conversion: Conversion = CaptureConversions.DEFAULT) = {
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
  def apply[A](value: A, reification: Reification[A]): ReifiedValue[A] = {
    if (value.isInstanceOf[Function1[_, _]] &&
        reification.taggedExpr.tree.isInstanceOf[scala.reflect.api.Trees#Function]) {
      new ReifiedFunction[Any, Any](
        value.asInstanceOf[Function[Any, Any]], 
        reification.asInstanceOf[Reification[Any => Any]]
      ).asInstanceOf[ReifiedValue[A]]
    } else {
      new ReifiedValue[A](value, reification)
    }
  }
}

