package scalaxy.fx

import scala.language.implicitConversions
import scala.language.experimental.macros

import scala.reflect.ClassTag

import javafx.beans.value._

/** Meant to be imported by (package) objects that want to expose change listener macros. */
private[fx] trait ObservableValueExtensions
{
  /** Methods on observable values */
  implicit def observableValuesExtensions[T](value: ObservableValue[T]) = new
  {
    // /** Add change listener to the observable value using a function
    //  *  that takes the new value.
    //  */
    // def onChange[V <: T](f: V => Unit): Unit =
    //   macro impl.ObservableValueExtensionMacros.onChangeFunction[V]

    /** Add change listener to the observable value using a function
     *  that takes the old value and the new value.
     */
    def onChangeWithValues[V <: T](f: (V, V) => Unit): Unit =
      macro impl.ObservableValueExtensionMacros.onChangeFunction2[V]

    /** Add change listener to the observable value using a block (passed `by name`). */
    def onChange(block: Unit): Unit =
      macro impl.ObservableValueExtensionMacros.onChangeBlock[Any]

    /** Add invalidation listener using a block (passed `by name`) */
    def onInvalidate(block: Unit): Unit =
      macro impl.ObservableValueExtensionMacros.onInvalidate[Any]
  }
}
