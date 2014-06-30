package scalaxy.fx

import scala.language.implicitConversions

import javafx.beans.binding._
import javafx.beans.property._

import scala.language.experimental.macros

private[fx] trait Properties
{
  /** Creates a simple property of type T. */
  def newProperty
      [T, J, B <: Binding[J], P <: Property[J]]
      (value: T)
      (implicit ev: GenericType[T, J, B, P]): P =
    macro impl.PropertyMacros.newProperty[T, P]

  implicit def propertyValue(p: SimpleIntegerProperty): Int =
    macro impl.PropertyMacros.propertyValue[Int, SimpleIntegerProperty]

  implicit def propertyValue(p: SimpleLongProperty): Long =
    macro impl.PropertyMacros.propertyValue[Long, SimpleLongProperty]

  implicit def propertyValue(p: SimpleFloatProperty): Float =
    macro impl.PropertyMacros.propertyValue[Float, SimpleFloatProperty]

  implicit def propertyValue(p: SimpleDoubleProperty): Double =
    macro impl.PropertyMacros.propertyValue[Double, SimpleDoubleProperty]

  implicit def propertyValue(p: SimpleBooleanProperty): Boolean =
    macro impl.PropertyMacros.propertyValue[Boolean, SimpleBooleanProperty]


  implicit def bindingValue(b: IntegerBinding): Int =
    macro impl.PropertyMacros.bindingValue[Int, IntegerBinding]

  implicit def bindingValue(b: LongBinding): Long =
    macro impl.PropertyMacros.bindingValue[Long, LongBinding]

  implicit def bindingValue(b: FloatBinding): Float =
    macro impl.PropertyMacros.bindingValue[Float, FloatBinding]

  implicit def bindingValue(b: DoubleBinding): Double =
    macro impl.PropertyMacros.bindingValue[Double, DoubleBinding]

  implicit def bindingValue(b: BooleanBinding): Boolean =
    macro impl.PropertyMacros.bindingValue[Boolean, BooleanBinding]

}
