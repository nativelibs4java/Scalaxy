package scalaxy.js
import scala.reflect.api.Universe

class global extends scala.annotation.StaticAnnotation

object global {
  def hasAnnotation(u: Universe)(sym: u.Symbol): Boolean = sym != null && {
    sym.annotations.exists(a => a.tpe != null && a.tpe =:= u.typeOf[scalaxy.js.global])
  }
}
