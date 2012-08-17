package scalaxy; package rewrites

import macros._
import matchers._
//import scala.reflect.mirror._

object ForLoops {
  /*
  def simpleForeachUntil[U](start: Int, end: Int, body: U) = replace(
    for (i <- start until end) 
      body,
    {
      var ii = start
      while (ii < end) {
        val i = ii
        body
        ii = ii + 1  
      }
    }
  )
  def simpleForeachTo[U](start: Int, end: Int, body: U) = replace(
    for (i <- start to end) body,
    {
      var ii = start
      while (ii <= end) {
        val i = ii
        body
        ii = ii + 1  
      }
    }
  )
  
  def rgForeachUntilBy[U](start: Int, end: Int, step: Int, body: U) = 
    when(
      for (i <- start until end by step) 
        body
    )(
      step
    ) {
      case PositiveIntConstant(_) :: Nil =>
        replacement {
          var ii = start
          while (ii < end) {
            val i = ii
            body
            ii = ii + step 
          }
        }
      case NegativeIntConstant(_) :: Nil =>
        replacement {
          var ii = start
          while (ii > end) {
            val i = ii
            body
            ii = ii + step 
          }
        }
      case _ =>
        warning("Cannot optimize : step is not constant")
    }
  */
}
