package scalaxy.fx
package impl

import javafx.beans._
import javafx.beans.binding._
import javafx.beans.value._

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

private[fx] object PropertyGettersMacros
{
  def get[A : c.WeakTypeTag](c: Context): c.Expr[A] = {
    import c.universe._

    val q"${_}($target).$name" = c.macroApplication
    c.Expr[A](q"$target.${TermName(name + "Property")}.get")
  }

  def set[A : c.WeakTypeTag](c: Context)(value: c.Expr[A]): c.Expr[Unit] = {
    import c.universe._

    val q"${_}($target).$name(${_})" = c.macroApplication
    val propertyName = TermName(
      name.decodedName.toString.replaceAll("_=$", "") + "Property")
    c.Expr[Unit](
      q"$target.$propertyName.set($value)")
  }
}
