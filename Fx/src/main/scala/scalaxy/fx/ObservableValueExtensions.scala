package scalaxy.fx

import javafx.beans.value._

import scala.language.experimental.macros

/** Meant to be imported by (package) objects that want to expose change listener macros. */
private[fx] trait ObservableValueExtensions
{
  /** Methods on observable values */
  implicit def observableValuesExtensions[T](value: ObservableValue[T]) = new
  {
    /** Add change listener to the observable value using a function
     *  that takes the new value.
     */
    def onChange(f: T => Unit): Unit =
      macro impl.ObservableValueExtensionMacros.onChangeFunction[T]

    /** Add change listener to the observable value using a function
     *  that takes the old value and the new value.
     */
    def onChange(f: (T, T) => Unit): Unit =
      macro impl.ObservableValueExtensionMacros.onChangeFunction2[T]

    /** Add change listener to the observable value using a block (passed `by name`). */
    def onChange(block: Unit): Unit =
      macro impl.ObservableValueExtensionMacros.onChangeBlock[T]

    /** Add invalidation listener using a block (passed `by name`) */
    def onInvalidate(block: Unit): Unit =
      macro impl.ObservableValueExtensionMacros.onInvalidate[T]
  }
}
