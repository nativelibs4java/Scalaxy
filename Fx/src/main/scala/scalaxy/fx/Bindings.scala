package scalaxy.fx

import javafx.beans.binding.Binding
import javafx.beans.property.Property

import scala.language.experimental.macros

/** Meant to be imported by (package) objects that want to expose binding macros. */
private[fx] trait Bindings {
  /** Create a binding with the provided expression,
   *  which is scavenged for observable dependencies.
   */
  def bind[T, J, B <: Binding[J], P <: Property[J]](expression: T)(implicit ev: GenericType[T, J, B, P]): B =
    macro impl.BindingMacros.bindImpl[T, J, B, P]
}
