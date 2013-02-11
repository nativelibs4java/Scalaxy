package scalaxy.compilets

import matchers._

object JavaDeprecations extends Compilet {

  def warnAccessibleField(f: java.lang.reflect.Field, b: Boolean) =
    when(f.setAccessible(b))(b) {
      case True() :: Nil =>
        warning("You shouldn't do that")
      case r =>
        println("Failed to match case in warnAccessibleField : " + r)
        null
    }

  def forbidThreadStop(t: Thread) =
    fail("You must NOT call Thread.stop() !") {
      t.stop
    }

}
