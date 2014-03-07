package scalaxy.loops
package test

import org.junit._
import Assert._

import scala.reflect.runtime.{ universe => ru }
import scala.reflect.runtime.{ currentMirror => cm }
import scala.tools.reflect.ToolBox

class StreamComponentsTestBase {
  val global = ru
  val toolbox = cm.mkToolBox()

  import global._

  object S {
    def unapply(symbol: Symbol): Option[String] =
      Option(symbol).map(_.name.toString)
  }
  object N {
    def unapply(name: Name): Option[String] =
      Option(name).map(_.toString)
  }
  def typeCheck(t: Tree, tpe: Type = WildcardType): Tree =
    toolbox.typeCheck(t.asInstanceOf[toolbox.u.Tree], tpe.asInstanceOf[toolbox.u.Type]).asInstanceOf[Tree]

  override def typed(tree: Tree, tpe: Type) = typeCheck(tree, tpe)
}
