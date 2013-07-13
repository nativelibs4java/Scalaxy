package scalaxy.fx

import javafx.beans.binding._
import javafx.beans.property._

import scala.language.experimental.macros

/** This trait is just here to let the typer associate value types T with:
 *  - Their Binding[T] subclass (IntegerBinding...),
 *  - Their Property[T] subclass (SimpleIntegerProperty...)
 *  It cannot be instantiated and may not be used at runtime (all references
 *  to it are meant to be removed by macros at compilation-time).
 */
private[fx] sealed trait GenericType[T, J, B <: Binding[J], P <: Property[J]]

/** Type associations. */
private[fx] trait GenericTypes 
{
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
}
