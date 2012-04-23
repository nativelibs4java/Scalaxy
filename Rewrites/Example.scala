package scalaxy; package rewrites

import macros._

object Example {
  
  def intToStringQuoter[U](i: Int) = replace(
    i.toString,
    "'" + i.toString + "'"
  )
}
