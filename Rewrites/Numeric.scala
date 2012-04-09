package scalaxy; package rewrites

import Macros._

object Numeric {
  import math.Numeric.Implicits._
  import Ordering.Implicits._
  
  def plus[T](a: T, b: T)(implicit n: Numeric[T]) = Replacement(
    a + b,
    n.plus(a, b)
  )
  
  def minus[T](a: T, b: T)(implicit n: Numeric[T]) = Replacement(
    a - b,
    n.minus(a, b)
  )
  
  def times[T](a: T, b: T)(implicit n: Numeric[T]) = Replacement(
    a * b,
    n.times(a, b)
  )
  
  def negate[T](a: T)(implicit n: Numeric[T]) = Replacement(
    - a,
    n.negate(a)
  )
  
  def gt[T](a: T, b: T)(implicit n: Numeric[T]) = Replacement(
    a > b,
    n.gt(a, b)
  )
  
  def gteq[T](a: T, b: T)(implicit n: Numeric[T]) = Replacement(
    a >= b,
    n.gteq(a, b)
  )
  
  def lt[T](a: T, b: T)(implicit n: Numeric[T]) = Replacement(
    a < b,
    n.lt(a, b)
  )
  
  def lteq[T](a: T, b: T)(implicit n: Numeric[T]) = Replacement(
    a <= b,
    n.lteq(a, b)
  )
}
