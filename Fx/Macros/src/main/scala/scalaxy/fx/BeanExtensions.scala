package scalaxy.fx

import scala.language.dynamics
import scala.language.experimental.macros

/** Meant to be imported by (package) objects that want to expose event handler macros. */
private[fx] trait BeanExtensions
{
  /** This adds `obj.set(property1 = value1, property2 = value2)` to all object types.
   *  Properties of type EventHandler[_] benefit from a special type-check to accommodate
   *  implicit conversions below.
   */
  implicit def beansExtensions[T](bean: T) = new {
    def set = new Dynamic {
      def applyDynamicNamed(name: String)(args: (String, Any)*): T =
        macro impl.BeanExtensionMacros.applyDynamicNamedImpl[T]
    }
  }
}
