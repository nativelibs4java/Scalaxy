package scalaxy; package compilets

import scala.reflect.runtime.universe._

import macros._
import matchers._
//import scala.reflect.mirror._

object SingleForLoop extends Compilet {
  def simpleForeachUntil[U : TypeTag](start: Int, end: Int, body: U) = replace(
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
}
