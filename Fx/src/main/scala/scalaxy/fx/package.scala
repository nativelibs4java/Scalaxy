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

package object fx extends GenericTypes with EventHandlers
{
  /** This adds `obj.set(property1 = value1, property2 = value2)` to all object types.
   *  Properties of type EventHandler[_] benefit from a special type-check to accommodate 
   *  implicit conversions below.
   */
  implicit def fxBeansExtensions[T](bean: T) = new {
    def set = new Dynamic {
      def applyDynamicNamed(name: String)(args: (String, Any)*): T =
        macro BeansMacros.applyDynamicNamedImpl[T]
    }
  }
  
  /** Create a binding with the provided expression, 
   *  which is scavenged for observable dependencies.
   */
  def bind[T, J, B <: Binding[J], P <: Property[J]]
      (expression: T)
      (implicit ev: GenericType[T, J, B, P]): B =
    macro BindingMacros.bindImpl[T, J, B, P]
  
}
