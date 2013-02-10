package scalaxy.fx.runtime

import javafx.beans._
import javafx.beans.binding._
import javafx.beans.property._
import javafx.beans.value._
import javafx.event._

abstract class ScalaChangeListener[T] extends ChangeListener[T] {
  override def changed(observable: ObservableValue[_ <: T], oldValue: T, newValue: T) {
    changed(oldValue, newValue)
  }
  
  def changed(oldValue: T, newValue: T): Unit
}
