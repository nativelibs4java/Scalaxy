package scalaxy; package rewrites

import Macros._

object Example {
  
  def intToStringQuoter[U](i: Int) = Replacement(
    i.toString,
    "'" + i.toString + "'"
  )
}
