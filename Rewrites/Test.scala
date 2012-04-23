package scalaxy; package rewrites

import macros._
import matchers._

object Test 
{  
  def removeDevilConstant = replace(666, 667)
  
  def replace888Constant(v: Int) = 
    when(v)(v) { 
      case IntConstant(888) :: Nil =>
        replacement(999)
    }
}
