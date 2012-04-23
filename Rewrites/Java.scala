package scalaxy.rewrites

object Java {
  import scalaxy._
  import matchers._
  import macros._
  
  
  def warnAccessibleField(f: java.lang.reflect.Field, b: Boolean) =
    when(f.setAccessible(b))(f, b) {
      case Seq(_, True()) =>
        warning("You shouldn't do that")
      case r => 
        println("Failed to match case in warnAccessibleField : " + r)
        null
    }
    
  def forbidThreadStop(t: Thread) = 
    fail("You must NOT call Thread.stop() !") {
      t.stop
    }
  
  //def replaceAccessibleField(f: java.lang.reflect.Field, b: Boolean) =
  //  replace(f.setAccessible(b), f.setAccessible(false))
    
    
    
  println("Auto warn " + warnAccessibleField(null, false))
}  
