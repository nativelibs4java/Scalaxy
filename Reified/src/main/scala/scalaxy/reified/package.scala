package scalaxy

import scala.language.experimental.macros
import scala.language.implicitConversions

import scala.reflect._
import scala.reflect.macros.Context
import scala.reflect.runtime
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.{ typeTag, TypeTag }

import scalaxy.reified.internal

/**
 * Scalaxy/Reified: the reify method in this package captures it's compile-time argument's AST,
 * allowing / preserving values captured outside its expression.
 */
package object reified {

  /**
   * Reify a value (including functions), preserving the original value and keeping track of the
   * values it captures from the scope of its expression.
   * This allows for runtime processing of the value's AST (being able to capture external values makes this method more flexible than Universe.reify).
   * Compile-time error are raised when an external reference cannot be captured safely (vars and
   * lazy vals are not considered safe, for instance).
   * Captured values are inlined in the reified value's AST with a conversion function,
   * which can be customized (by default, it handles constants, arrays, immutable collections,
   * reified values and their wrappers).
   */
  def reify[A: TypeTag](v: A): ReifiedValue[A] = macro internal.reifyImpl[A]

  /**
   * Wrapper that provides Function1-like methods to a reified Function1 value.
   *
   * @param value reified function value
   */
  implicit class ReifiedFunction1[T1: TypeTag, R: TypeTag](
    val value: ReifiedValue[T1 => R])
      extends HasReifiedValue[T1 => R] {

    assert(value != null)

    override def reifiedValue = value
    override def valueTag = typeTag[T1 => R]

    /** Evaluate this function using the regular, non-reified runtime value */
    def apply(a: T1): R = value.value(a)

    def compose[A: TypeTag](g: ReifiedFunction1[A, T1]): ReifiedFunction1[A, R] = {
      val f = this
      internal.reifyMacro((c: A) => f(g(c)))
    }

    def andThen[A: TypeTag](g: ReifiedFunction1[R, A]): ReifiedFunction1[T1, A] = {
      val f = this
      internal.reifyMacro((a: T1) => g(f(a)))
    }
  }

  /**
   * Wrapper that provides Function2-like methods to a reified Function2 value.
   *
   * @param value reified function value
   */
  implicit class ReifiedFunction2[T1: TypeTag, T2: TypeTag, R: TypeTag](
    val value: ReifiedValue[Function2[T1, T2, R]])
      extends HasReifiedValue[Function2[T1, T2, R]] {

    assert(value != null)

    override def reifiedValue = value
    override def valueTag = typeTag[Function2[T1, T2, R]]

    /** Evaluate this function using the regular, non-reified runtime value */
    def apply(v1: T1, v2: T2): R = value.value(v1, v2)

    /*
    // TODO fix this:
    def curried: ReifiedFunction1[T1, ReifiedFunction1[T2, R]] = {
      val f = this
      def finish(v1: T1) = {
        base.reify((v2: T2) => {
          f(v1, v2)
        })
      }
      base.reify((v1: T1) => finish(v1))
    }
    */

    def tupled: ReifiedFunction1[(T1, T2), R] = {
      val f = this
      internal.reifyMacro((p: (T1, T2)) => {
        val (v1, v2) = p
        f(v1, v2)
      })
    }
  }

  /**
   * Implicitly extract reified value from its wrappers (such as ReifiedFunction1, ReifiedFunction2).
   */
  implicit def hasReifiedValueToReifiedValue[A](r: HasReifiedValue[A]): ReifiedValue[A] = {
    r.reifiedValue
  }

  /**
   * Implicitly convert reified value to their original non-reified value.
   */
  implicit def hasReifiedValueToValue[A](r: HasReifiedValue[A]): A = r.reifiedValue.value
}

