package scalaxy; package compilets

import scala.reflect.runtime.universe._

import macros._

object Numerics {
  import math.Numeric.Implicits._
  import Ordering.Implicits._
  
  def plus[T : TypeTag : Numeric](a: T, b: T) = replace(
    a + b, // Numeric.Implicits.infixNumericOps[T : TypeTag](a)(implicitly[Numeric[T]]).+(b)
    implicitly[Numeric[T]].plus(a, b)
  )
  
  def minus[T : TypeTag : Numeric](a: T, b: T) = replace(
    a - b,
    implicitly[Numeric[T]].minus(a, b)
  )
  
  def times[T : TypeTag : Numeric](a: T, b: T) = replace(
    a * b,
    implicitly[Numeric[T]].times(a, b)
  )
  
  def negate[T : TypeTag : Numeric](a: T) = replace(
    - a,
    implicitly[Numeric[T]].negate(a)
  )
  
  def gt[T : TypeTag : Numeric](a: T, b: T) = replace(
    a > b,
    implicitly[Numeric[T]].gt(a, b)
  )
  
  def gteq[T : TypeTag : Numeric](a: T, b: T) = replace(
    a >= b,
    implicitly[Numeric[T]].gteq(a, b)
  )
  
  def lt[T : TypeTag : Numeric](a: T, b: T) = replace(
    a < b,
    implicitly[Numeric[T]].lt(a, b)
  )
  
  def lteq[T : TypeTag : Numeric](a: T, b: T) = replace(
    a <= b,
    implicitly[Numeric[T]].lteq(a, b)
  )
}
