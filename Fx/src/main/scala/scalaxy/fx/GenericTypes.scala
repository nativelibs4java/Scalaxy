package scalaxy.fx

import javafx.beans._
import javafx.beans.property._
import javafx.beans.value._
import javafx.beans.binding._

import scala.language.experimental.macros

/** This trait is just here to play with the typer: it has no implementation. */
private[fx] sealed trait GenericType[T, J, B <: Binding[J], P <: Property[J]]

/** Meant to be imported by (package) objects that want to expose binding and property macros. */
private[fx] trait GenericTypes {
  // Associate types with their corresponding JavaFX Binding and Property subclasses.
  implicit def GenericObjectType[T <: AnyRef]: 
      GenericType[T, T, Binding[T], SimpleObjectProperty[T]] = ???
      
  implicit def GenericIntegerType: 
      GenericType[Int, Number, IntegerBinding, SimpleIntegerProperty] = ???
      
  implicit def GenericLongType: 
      GenericType[Long, Number, LongBinding, SimpleLongProperty] = ???
      
  implicit def GenericFloatType:
      GenericType[Float, Number, FloatBinding, SimpleFloatProperty] = ???
      
  implicit def GenericDoubleType: 
      GenericType[Double, Number, DoubleBinding, SimpleDoubleProperty] = ???
      
  implicit def GenericBooleanType: 
      GenericType[Boolean, java.lang.Boolean, BooleanBinding, SimpleBooleanProperty] = ???

  /** Creates a simple property of type T. */
  def property
      [T, J, B <: Binding[J], P <: Property[J]]
      (value: T)
      (implicit ev: GenericType[T, J, B, P]): P =
    macro impl.GenericTypeMacros.newProperty[T, P]

  /** Implicit conversion from property to value. */
  implicit def propertyValue
      [T, J, B <: Binding[J], P <: Property[J]]
      (p: P)
      (implicit ev: GenericType[T, J, B, P]): T =
    macro impl.GenericTypeMacros.propertyValue[T, P]

  /** Implicit conversion from binding to value. */
  implicit def bindingValue
      [T, J, B <: Binding[J], P <: Property[J]]
      (b: B)
      (implicit ev: GenericType[T, J, B, P]): T =
    macro impl.GenericTypeMacros.bindingValue[T, B]
}
