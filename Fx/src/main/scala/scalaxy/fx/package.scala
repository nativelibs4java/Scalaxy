package scalaxy

import javafx.beans._
import javafx.beans.value._
import javafx.beans.property._
import javafx.beans.binding._
import javafx.event._
import javafx.scene._

import scala.language.dynamics
import scala.language.experimental.macros
import scala.reflect.NameTransformer
import scala.reflect.macros.Context

// TODO: rewrite all calls with macros to avoid dependency to this package object?
package object fx extends GenericTypes
{
  // This adds `obj.set(property1 = value1, property2 = value2)` to all object types.
  // Properties of type EventHandler[_] benefit from a special type-check to accommodate 
  // implicit conversions below.
  implicit def fxBeansExtensions[T](bean: T) = new {
    def set = new Dynamic {
      def applyDynamicNamed(name: String)(args: (String, Any)*): T =
        macro BeansMacros.applyDynamicNamedImpl[T]
    }
  }
  
  // Create a binding with the provided expression, which is scavenged for observable dependencies.
  def bind[T, J, B <: Binding[J], P <: Property[J]]
      (expression: T)
      (implicit ev: GenericType[T, J, B, P]): B =
    macro BindingMacros.bindExpressionImpl[T, J, B, P]
  
  // Implicit conversion from an event handler function to a JavaFX EventHandler[_].
  implicit def functionHandler[E <: Event](f: E => Unit): EventHandler[E] =
    macro EventHandlerMacros.functionHandlerImpl[E]
  
  // Implicit conversion from an event handler block to a JavaFX EventHandler[_].
  implicit def blockHandler[E <: Event](block: Unit): EventHandler[E] =
    macro EventHandlerMacros.blockHandlerImpl[E]
}
