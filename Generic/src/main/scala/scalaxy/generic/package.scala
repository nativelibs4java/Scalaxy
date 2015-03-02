package scalaxy

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

import scala.language.implicitConversions
import scala.language.dynamics

import scala.reflect.ClassTag
import scala.reflect.NameTransformer
import scala.reflect.runtime.universe._
import scala.reflect.runtime.currentMirror

//import scalaxy.debug._

// import scalaxy.union._ //`|`
/**
 * Type used to model constraint alternatives in Generic
 */
// trait |[A, B]

package object generic {
  //type TypeTag[T] = scala.reflect.runtime.universe.TypeTag[T]

  def generic[A: Generic](value: A) = new GenericOps[A](value)

  private[generic] val typeTagsToNumerics: Map[scala.reflect.runtime.universe.TypeTag[_], Numeric[_]] = {
    import Numeric._
    Map(
      TypeTag.Byte -> implicitly[Numeric[Byte]],
      TypeTag.Short -> implicitly[Numeric[Short]],
      TypeTag.Int -> implicitly[Numeric[Int]],
      TypeTag.Long -> implicitly[Numeric[Long]],
      TypeTag.Float -> implicitly[Numeric[Float]],
      TypeTag.Double -> implicitly[Numeric[Double]],
      TypeTag.Char -> implicitly[Numeric[Char]],
      typeTag[math.BigInt] -> implicitly[Numeric[math.BigInt]],
      typeTag[math.BigDecimal] -> implicitly[Numeric[math.BigDecimal]]
    )
  }

  def genericTypeTag[A: Generic]: TypeTag[A] = implicitly[Generic[A]].typeTag

  implicit def mkNumeric[A: TypeTag]: Option[Numeric[A]] = {
    val numOpt = typeTagsToNumerics.get(implicitly[TypeTag[A]])
    numOpt.map(_.asInstanceOf[Numeric[A]])
  }

  implicit def numeric[A: Generic] = implicitly[Generic[A]].numeric.getOrElse(sys.error("This generic has no associated numeric"))

  def zero[A: Generic]: A = numeric[A].zero
  def one[A: Generic]: A = numeric[A].one
  def number[A: Generic](value: Int): A = numeric[A].fromInt(value)

  // implicit def mkGenericOps[A: Generic](value: GenericOps[_]): GenericOps[A] = new GenericOps[A](value.value.asInstanceOf[A])
  implicit def mkGenericOps[A: Generic](value: A): GenericOps[A] = new GenericOps[A](value)

  //def generic[A: Generic](value: A, implicitConversions: Any*) = new GenericOps[A](value, implicitConversions)
}
