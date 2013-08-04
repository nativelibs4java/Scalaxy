package scalaxy

import scala.language.experimental.macros
import scala.reflect.macros.Context

import scala.language.implicitConversions
import scala.language.dynamics

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

// import scalaxy.union._ //`|`
package generic {
  /**
   * Type used to model constraint alternatives in Generic
   */
  // trait |[A, B]

  sealed class Generic[A](val numeric: Option[Numeric[A]] = None)
}

package object generic {

  // type NumericType = Byte | Short | Int | Long | Float | Double

  implicit def genericNumericInstance[A <: AnyVal: Numeric]: Generic[A] = new Generic[A](Some(implicitly[Numeric[A]]))
  implicit def genericInstance[A <: AnyRef]: Generic[A] = new Generic[A]()

  def numeric[A: Generic] = implicitly[Generic[A]].numeric.getOrElse(sys.error("This generic has no associated numeric"))

  def zero[A: Generic]: A = numeric[A].zero
  def one[A: Generic]: A = numeric[A].one
  def number[A: Generic](value: Int): A = numeric[A].fromInt(value)

  implicit def mkGenericOps[A: Generic](value: GenericOps[_]): GenericOps[A] = new GenericOps[A](value.value.asInstanceOf[A])
  implicit def mkGenericOps[A: Generic](value: A): GenericOps[A] = new GenericOps[A](value)

  def generic[A: Generic](value: A, implicitConversions: Any*) = new GenericOps[A](value, implicitConversions)
}
