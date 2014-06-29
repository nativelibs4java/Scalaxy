package scalaxy.fx
package impl

import javafx.beans._
import javafx.beans.binding._
import javafx.beans.value._

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

private[fx] object PropertyMacros
{
  def newProperty
      [T : c.WeakTypeTag, P : c.WeakTypeTag]
      (c: Context)
      (value: c.Expr[T])
      (ev: c.Expr[GenericType[_, _, _, _]]): c.Expr[P] =
  {
    import c.universe._
    c.Expr[P](q"new ${weakTypeOf[P]}($value)")
  }

  def propertyValue
      [T : c.WeakTypeTag, P : c.WeakTypeTag]
      (c: Context)
      (p: c.Expr[P])
      (ev: c.Expr[GenericType[_, _, _, _]]): c.Expr[T] =
  {
    import c.universe._
    c.Expr[T](q"$p.getValue")
  }

  def bindingValue
      [T : c.WeakTypeTag, B : c.WeakTypeTag]
      (c: Context)
      (b: c.Expr[B])
      (ev: c.Expr[GenericType[_, _, _, _]]): c.Expr[T] =
  {
    import c.universe._
    c.Expr[T](q"$b.getValue")
  }
}
