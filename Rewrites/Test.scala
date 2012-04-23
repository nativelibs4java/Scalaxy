package scalaxy; package rewrites

import macros._
import matchers._

object Test {
  /*
  def removeDevilConstant(v: Int) = 
    when(v)(v) { 
      case IntConstant(666) :: Nil =>
        replacement(667)
    }
  */
  def removeDevilConstant = replace(666, 667)
}
