package scalaxy.fx

import javafx.beans.binding.Binding
import javafx.beans.property.Property

import scala.language.experimental.macros

private[fx] trait Properties
{
  /** Creates a simple property of type T. */
  def property
      [T, J, B <: Binding[J], P <: Property[J]]
      (value: T)
      (implicit ev: GenericType[T, J, B, P]): P =
    macro impl.PropertyMacros.newProperty[T, P]

  /** Implicit conversion from property to value. */
  implicit def propertyValue
      [T, J, B <: Binding[J], P <: Property[J]]
      (p: P)
      (implicit ev: GenericType[T, J, B, P]): T =
    macro impl.PropertyMacros.propertyValue[T, P]

  /** Implicit conversion from binding to value. */
  implicit def bindingValue
      [T, J, B <: Binding[J], P <: Property[J]]
      (b: B)
      (implicit ev: GenericType[T, J, B, P]): T =
    macro impl.PropertyMacros.bindingValue[T, B]
}
