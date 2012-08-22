package scalaxy; package compilets

import scala.reflect.mirror._

import macros._

object Example {
  
  def intToStringQuoter[U : TypeTag](i: Int) = replace(
    i.toString,
    "'" + i.toString + "'"
  )
}
