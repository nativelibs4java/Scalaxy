package scalaxy.fx

import scala.language.experimental.macros

import javafx.beans._
import javafx.beans.property._
import javafx.beans.value._
import javafx.beans.binding._
import javafx.event._

/** Meant to be imported by (package) objects that want to expose binding macros. */
private[fx] trait Bindings 
{
  /** Create a binding with the provided expression, 
   *  which is scavenged for observable dependencies.
   */
  def bind[T, J, B <: Binding[J], P <: Property[J]]
      (expression: T)
      (implicit ev: GenericType[T, J, B, P]): B =
    macro BindingMacros.bindImpl[T, J, B, P]
}
