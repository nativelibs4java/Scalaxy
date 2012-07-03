package scalaxy.inlinable

import language.experimental.macros
import reflect.makro.Context

trait InlinableNames
{
  val universe: reflect.makro.Universe
  import universe._

  class N(val s: String*) {
    def unapply(n: Name): Boolean = s.contains(n.toString)
  }
  object N {
    def apply(s: String*) = new N(s: _*)
  }
  implicit def N2Name(n: N) = newTermName(n.s(0))

  val intWrapperName = N("intWrapper")
  val toName = N("to", "to_")
  val untilName = N("until", "until_")
  val byName = N("by")
}
