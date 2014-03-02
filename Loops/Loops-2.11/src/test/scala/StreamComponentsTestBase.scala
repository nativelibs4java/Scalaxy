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

  def typeCheck(t: Tree): Tree =
    toolbox.typeCheck(t.asInstanceOf[toolbox.u.Tree]).asInstanceOf[Tree]
}
