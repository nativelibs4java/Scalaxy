package scalaxy; package rewrites

import Macros._

object ForLoops {
  def simpleForeachUntil[U](start: Int, end: Int, body: U) = Replacement(
    for (i <- start until end) body,
    {
      var ii = start
      while (ii < end) {
        val i = ii
        body
        ii = ii + 1  
      }
    }
  )
  def simpleForeachTo[U](start: Int, end: Int, body: U) = Replacement(
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
  /*
  def simpleForeachUntil[U](start: Int, end: Int, body: Int => U) = Replacement(
    for (i <- start until end) body(i),
    {
      var ii = start
      while (ii < end) {
        val i = ii
        body(i)
        ii = ii + 1  
      }
    }
  )
  def simpleForeachTo[U](start: Int, end: Int, body: Int => U) = Replacement(
    for (i <- start to end) body(i),
    {
      var ii = start
      while (ii <= end) {
        val i = ii
        body(i)
        ii = ii + 1  
      }
    }
  )
  */
}
