package scalaxy

import scala.language.experimental.macros
import scala.reflect.macros.Context

import scala.language.implicitConversions
import scala.language.dynamics

import scala.reflect.ClassTag
// import scala.reflect.runtime.universe.TypeTag

// import scalaxy.union._ //`|`
package generic {
  /**
   * Type used to model constraint alternatives in Generic
   */
  // trait |[A, B]

  sealed class Generic[A: TypeTag] {
    def typeTag = implicitly[TypeTag[A]]
    def numeric: Option[Numeric[A]] = implicitly[Option[Numeric[A]]]
  }
}

package object generic {
  type TypeTag[T] = scala.reflect.runtime.universe.TypeTag[T]

  private val typeTagsToNumerics: Map[scala.reflect.runtime.universe.TypeTag[_], Numeric[_]] = {
    import scala.reflect.runtime.universe._
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

  implicit def mkNumeric[A: TypeTag]: Option[Numeric[A]] = {
    // println(typeTagsToNumerics)
    val numOpt = typeTagsToNumerics.get(implicitly[TypeTag[A]])
    numOpt.map(_.asInstanceOf[Numeric[A]])
  }

  implicit def mkGeneric[A: TypeTag]: Generic[A] = new Generic[A]

  implicit def numeric[A: Generic] = implicitly[Generic[A]].numeric.getOrElse(sys.error("This generic has no associated numeric"))

  def zero[A: Generic]: A = numeric[A].zero
  def one[A: Generic]: A = numeric[A].one
  def number[A: Generic](value: Int): A = numeric[A].fromInt(value)

  implicit def mkGenericOps[A: Generic](value: GenericOps[_]): GenericOps[A] = new GenericOps[A](value.value.asInstanceOf[A])
  implicit def mkGenericOps[A: Generic](value: A): GenericOps[A] = new GenericOps[A](value)

  def generic[A: Generic](value: A, implicitConversions: Any*) = new GenericOps[A](value, implicitConversions)
}
