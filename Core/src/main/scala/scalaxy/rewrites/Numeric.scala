package scalaxy; package rewrites

import Macros._

object Numeric {
  import math.Numeric.Implicits._
    
  def plus[T](a: T, b: T)(implicit n: Numeric[T]) = Replacement(
    a + b,
    n.plus(a, b)
  )
}
