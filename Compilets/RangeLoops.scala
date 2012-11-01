package scalaxy; package compilets

import macros._
import matchers._

object RangeLoops
{
  def foreachUntil[U](start: Int, end: Int, body: Int => U) = replace(
    for (i <- start until end) body(i),
    {
      var ii = start; val e = end
      while (ii < e) {
        val k = ii
        body(k)
        ii = ii + 1
      }
    }
  )

  def foreachTo[U](start: Int, end: Int, body: Int => U) = replace(
    for (i <- start to end) body(i),
    {
      var ii = start; val e = end
      while (ii <= e) {
        val i = ii
        body(i)
        ii = ii + 1
      }
    }
  )

  def foreachUntilBy[U](start: Int, end: Int, step: Int, body: Int => U) =
    when(for (i <- start until end by step) body(i))(
      step
    ) {
      case PositiveIntConstant(_) :: Nil =>
        replacement {
          var ii = start; val e = end
          while (ii < e) {
            val i = ii
            body(i)
            ii = ii + step
          }
        }
      case NegativeIntConstant(_) :: Nil =>
        replacement {
          var ii = start; val e = end
          while (ii > e) {
            val i = ii
            body(i)
            ii = ii + step
          }
        }
      case _ =>
        warning("Cannot optimize : step is not constant")
    }

  def foreachToBy[U](start: Int, end: Int, step: Int, body: Int => U) =
    when(for (i <- start to end by step) body(i))(
      step
    ) {
      case PositiveIntConstant(_) :: Nil =>
        replacement {
          var ii = start; val e = end
          while (ii <= e) {
            val i = ii
            body(i)
            ii = ii + step
          }
        }
      case NegativeIntConstant(_) :: Nil =>
        replacement {
          var ii = start; val e = end
          while (ii >= e) {
            val i = ii
            body(i)
            ii = ii + step
          }
        }
      case _ =>
        warning("Cannot optimize : step is not constant")
    }
}
