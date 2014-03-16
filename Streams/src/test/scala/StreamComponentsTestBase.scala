package scalaxy.streams
package test

import org.junit._
import org.junit.Assert._

import scala.collection.mutable.ListBuffer
import scala.tools.reflect.ToolBox
import scala.tools.reflect.FrontEnd

case class CompilerMessages(
  infos: List[String] = Nil,
  warnings: List[String] = Nil,
  errors: List[String] = Nil)

class StreamComponentsTestBase extends Utils {
  val global = scala.reflect.runtime.universe
  val commonOptions = "-usejavacp"
  import scala.reflect.runtime.currentMirror

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

  def typeCheck(t: global.Tree): global.Tree = {
    val toolbox = currentMirror.mkToolBox(options = commonOptions)
    toolbox.typeCheck(t.asInstanceOf[toolbox.u.Tree]).asInstanceOf[global.Tree]
  }

  def compile(source: String): (() => Any, CompilerMessages) = {
    val infosBuilder = ListBuffer[String]()
    val warningsBuilder = ListBuffer[String]()
    val errorsBuilder = ListBuffer[String]()
    val frontEnd = new FrontEnd {
      override def display(info: Info) {
        val builder: ListBuffer[String] = info.severity match {
          case INFO => infosBuilder
          case WARNING => warningsBuilder
          case ERROR => errorsBuilder
        }
        builder += info.msg
      }
      override def interactive() {}
    }
    val toolbox = currentMirror.mkToolBox(frontEnd = frontEnd, options = commonOptions)
    import toolbox.u._

    val tree = toolbox.parse(source);
    val compilation = toolbox.compile(tree)

    (
      compilation,
      CompilerMessages(
        infos = infosBuilder.result,
        warnings = warningsBuilder.result,
        errors = errorsBuilder.result)
    )
  }

  def assertMacroCompilesToSameValue(source: String): CompilerMessages = {
    val (unoptimized, unoptimizedMessages) = compile(source)
    val (optimized, optimizedMessages) = compile(s"scalaxy.streams.optimize { $source }");

    (unoptimized(), optimized()) match {
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
      case (expected: Array[Boolean], actual: Array[Boolean]) =>
        assertArrayEquals(source, expected.map(_.toString: AnyRef), actual.map(_.toString: AnyRef))
      case (expected, actual) =>
        assertEquals(source, expected, actual)
    }

    assertEquals("Unexpected messages during unoptimized compilation",
      CompilerMessages(), unoptimizedMessages)

    optimizedMessages
  }
}
