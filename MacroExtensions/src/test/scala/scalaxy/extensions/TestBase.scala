package scalaxy.extensions
package test

import org.junit._
import Assert._

import scalaxy.debug._

import java.io._

import scala.collection.mutable
import scala.reflect.internal.util._
import scala.tools.nsc._
import scala.tools.nsc.plugins._
import scala.tools.nsc.reporters._

trait TestBase {
  import MacroExtensionsCompiler.jarOf
  lazy val jars =
    jarOf(classOf[List[_]]).toSeq ++
    jarOf(classOf[scala.reflect.macros.Context]) ++
    jarOf(classOf[Global])

  def transform(code: String, name: String): String

  def assertSameTransform(original: String, equivalent: String) {
    val expected = transform(equivalent, "equiv")
    val actual = transform(original, "orig")
    if (actual != expected) {
      println(s"EXPECTED\n\t" + expected.replaceAll("\n", "\n\t"))
      println(s"ACTUAL\n\t" + actual.replaceAll("\n", "\n\t"))
      assertEquals(expected, actual)
    }
  }

  def expectException(reason: String)(block: => Unit) {
    try {
      block
      fail(s"Code should not have compiled: $reason")
    } catch { case ex: Throwable => }
  }

  //def normalize(s: String) = s.trim.replaceAll("^\\s*|\\s*?$", "")
  def transformCode(code: String, name: String, macroExtensions: Boolean, runtimeExtensions: Boolean): (String, String, Seq[(AbstractReporter#Severity, String)]) = {
    val settings0 = new Settings
    val file = {
      val file = File.createTempFile(name, ".scala")
      file.deleteOnExit()
      val out = new PrintWriter(file)
      out.print(code)
      out.close()

      file
    }
    try {
      val args = Array(file.toString)
      val command =
        new CompilerCommand(
          List("-bootclasspath", jars.mkString(File.pathSeparator)) ++ args, settings0)

      require(command.ok)

      val report = mutable.ArrayBuffer[(AbstractReporter#Severity, String)]()
      var transformed: (String, String) = null
      val reporter = new AbstractReporter {
        override val settings = settings0
        override def displayPrompt() {}
        override def display(pos: Position, msg: String, severity: Severity) {
          report += severity -> msg
        }
      }
      val global = new Global(settings0, reporter) {
        override protected def computeInternalPhases() {
          super.computeInternalPhases
          val comp = new MacroExtensionsComponent(this, macroExtensions = macroExtensions, runtimeExtensions = runtimeExtensions)
          phasesSet += comp
          // Get node string right after macro extensions component.
          phasesSet += new TestComponent(this, comp, (s, n) => transformed = s -> n)
          // Stop compilation after typer and refchecks, to see if there are errors.
          phasesSet += new StopComponent(this)
        }
      }
      new global.Run().compile(command.files)
      (transformed._1, transformed._2, report.toSeq)
    } finally {
      file.delete()
    }
  }

  class TestComponent(
      val global: Global,
      after: PluginComponent,
      out: (String, String) => Unit) extends PluginComponent
  {
    import global._

    override val phaseName = "after-" + after.phaseName
    override val runsRightAfter = Some(after.phaseName)
    override val runsAfter = runsRightAfter.toList
    override val runsBefore = List("patmat")

    def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {
      def apply(unit: CompilationUnit) {
        out(unit.body.toString, nodeToString(unit.body))
        unit.body = EmptyTree
      }
    }
  }

  class StopComponent(val global: Global) extends PluginComponent
  {
    import global._

    override val phaseName = "stop"
    override val runsRightAfter = Some("refchecks")
    override val runsAfter = runsRightAfter.toList
    override val runsBefore = Nil

    def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {
      def apply(unit: CompilationUnit) {
        unit.body = EmptyTree
      }
    }
  }
}
