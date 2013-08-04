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
  trait |[A, B]
}

package object generic {

  type NumericType = Byte | Short | Int | Long | Float | Double

  type Numeric[A] = Generic[A, NumericType]

  object Numeric {
    implicit def apply[N: math.Numeric](value: N) = new generic.Numeric(value)
  }

  def zero[N: math.Numeric]: generic.Numeric[N] = generic.Numeric(implicitly[math.Numeric[N]].zero)

  implicit def apply[A](value: A, implicitConversions: Any*) = new Generic[A, Any](value, implicitConversions)
  implicit def apply(value: Byte) = new generic.Numeric(value)
  implicit def apply(value: Short) = new generic.Numeric(value)
  implicit def apply(value: Int) = new generic.Numeric(value)
  implicit def apply(value: Long) = new generic.Numeric(value)
  implicit def apply(value: Float) = new generic.Numeric(value)
  implicit def apply(value: Double) = new generic.Numeric(value)

}
