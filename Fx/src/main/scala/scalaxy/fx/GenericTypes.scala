package scalaxy.fx

import scala.language.experimental.macros

import javafx.beans._
import javafx.beans.property._
import javafx.beans.value._
import javafx.beans.binding._

// Meant to be imported by (package) objects that want to expose binding and property macros.
trait GenericTypes {
  // Associate types with their corresponding JavaFX Binding and Property subclasses.
  implicit def GenericObjectType[T <: AnyRef]: GenericType[T, T, Binding[T], SimpleObjectProperty[T]] = ???
  implicit def GenericIntegerType: GenericType[Int, Number, IntegerBinding, SimpleIntegerProperty] = ???
  implicit def GenericLongType: GenericType[Long, Number, LongBinding, SimpleLongProperty] = ???
  implicit def GenericFloatType: GenericType[Float, Number, FloatBinding, SimpleFloatProperty] = ???
  implicit def GenericDoubleType: GenericType[Double, Number, DoubleBinding, SimpleDoubleProperty] = ??? 
  implicit def GenericBooleanType: GenericType[Boolean, java.lang.Boolean, BooleanBinding, SimpleBooleanProperty] = ???
  
  // Creates a simple property of type T.
  implicit def newProperty
      [T, J, B <: Binding[J], P <: Property[J]]
      (implicit ev: GenericType[T, J, B, P]): P = 
    macro GenericTypeMacros.newProperty[T, P]
  
  // Creates a binding with the provided value (passed by value, despite signature), 
  // depending on the provided observables.
  implicit def newBinding
      [T, J, B <: Binding[J], P <: Property[J]]
      (value: T, observables: Observable*)
      (implicit ev: GenericType[T, J, B, P]): P = 
    macro GenericTypeMacros.newBinding[T, B]
  
  // Implicit conversion from property to value.
  implicit def propertyValue
      [T, J, B <: Binding[J], P <: Property[J]]
      (p: P)
      (implicit ev: GenericType[T, J, B, P]): T = 
    macro GenericTypeMacros.propertyValue[T, P]
    
  // Implicit conversion from binding to value.
  implicit def bindingValue
      [T, J, B <: Binding[J], P <: Property[J]]
      (b: B)
      (implicit ev: GenericType[T, J, B, P]): T =  
    macro GenericTypeMacros.bindingValue[T, B]
}
