package scalaxy.js
import scala.reflect.api.Universe

class global extends scala.annotation.StaticAnnotation

trait Globals {

  val global: Universe
  import global._

  def hasGlobalAnnotation(sym: Symbol): Boolean = sym != null && {
    sym.annotations.exists(a => Option(a.tpe).exists(_ =:= typeOf[scalaxy.js.global]))
  }
}
