package scalaxy.fx

import scala.language.experimental.macros

import javafx.beans._
import javafx.beans.property._
import javafx.beans.value._
import javafx.beans.binding._
import javafx.event._

/** Meant to be imported by (package) objects that want to expose event handler macros. */
private[fx] trait ChangeListeners 
{
  /** Methods on observable values */
  implicit def observableValuesExtensions[T](value: ObservableValue[T]) = new 
  {
    /** Add change listener to the observable value using a function
     *  that takes the new value.
     */
    def onChange(f: T => Unit): Unit =
      macro ChangeListenerMacros.onChangeFunction[T]
    
    /** Add change listener to the observable value using a function
     *  that takes the old value and the new value.
     */
    def onChange(f: (T, T) => Unit): Unit =
      macro ChangeListenerMacros.onChangeFunction2[T]
    
    /** Add change listener to the observable value using a block (passed `by name`). */
    def onChange(block: Unit): Unit =
      macro ChangeListenerMacros.onChangeBlock[T]
    
    /** Add invalidation listener using a block (passed `by name`) */
    def onInvalidate(block: Unit): Unit =
      macro ChangeListenerMacros.onInvalidate[T]
  } 
}
