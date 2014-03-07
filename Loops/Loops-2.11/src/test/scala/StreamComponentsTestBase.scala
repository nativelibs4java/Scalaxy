package scalaxy.loops
package test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.{ universe => ru }
import scala.reflect.runtime.{ currentMirror => cm }
import scala.tools.reflect.ToolBox

class StreamComponentsTestBase extends Utils {
  val global = ru
  val toolbox = cm.mkToolBox(options = "-usejavacp")

  object S {
    import global._
    def unapply(symbol: Symbol): Option[String] =
      Option(symbol).map(_.name.toString)
  }
  object N {
    import global._
    def unapply(name: Name): Option[String] =
      Option(name).map(_.toString)
  }
  def typeCheck(t: global.Tree, tpe: global.Type = global.WildcardType): global.Tree =
    toolbox.typeCheck(t.asInstanceOf[toolbox.u.Tree], tpe.asInstanceOf[toolbox.u.Type]).asInstanceOf[global.Tree]

  override def typed(tree: global.Tree, tpe: global.Type) = typeCheck(tree, tpe)

  def assertMacroCompilesToSameValue(source: String) {
    import toolbox.u._

    val tree = toolbox.parse(source);
    val unoptimized = toolbox.eval(q"$tree");
    val optimized = toolbox.eval(q"scalaxy.loops.optimize { $tree }");

    (unoptimized, optimized) match {
      case (expected: Array[Int], actual: Array[Int]) =>
        assertArrayEquals(source, expected, actual)
      case (expected: Array[Short], actual: Array[Short]) =>
        assertArrayEquals(source, expected, actual)
      case (expected: Array[Byte], actual: Array[Byte]) =>
        assertArrayEquals(source, expected, actual)
      case (expected: Array[Char], actual: Array[Char]) =>
        assertArrayEquals(source, expected, actual)
      case (expected: Array[Float], actual: Array[Float]) =>
        assertArrayEquals(source, expected, actual, 0)
      case (expected: Array[Double], actual: Array[Double]) =>
        assertArrayEquals(source, expected, actual, 0)
      case (expected, actual) =>
        assertEquals(source, expected, actual)
    }
  }
}
