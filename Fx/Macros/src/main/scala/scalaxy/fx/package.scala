package scalaxy

/** Provides methods and implicit conversions that make it easy to use JavaFX from Scala.
 *  No runtime dependency is needed: all these methods are implemented using macros.
 */
package object fx
 extends BeanExtensions
    with Bindings
    with GenericTypes
    with EventHandlers
    with ObservableValueExtensions
    with Properties

