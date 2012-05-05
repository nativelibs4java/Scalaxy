package scalaxy; package rewrites

import macros._

object Example {
  
  def intToStringQuoter[U : TypeTag](i: Int) = replace(
    i.toString,
    "'" + i.toString + "'"
  )
}
