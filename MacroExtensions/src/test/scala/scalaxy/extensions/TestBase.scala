package scalaxy.extensions
package test

import org.junit._
import Assert._

import scalaxy.debug._

import java.io._

import scala.collection.mutable
import scala.tools.nsc.CompilerCommand
import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.ConsoleReporter

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
  def transformCode(code: String, name: String, macroExtensions: Boolean, runtimeExtensions: Boolean): (String, String) = {
    val settings = new Settings
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
          List("-bootclasspath", jars.mkString(File.pathSeparator)) ++ args, settings)
  
      require(command.ok)
      
      var transformed: (String, String) = null
      val global = new Global(settings, new ConsoleReporter(settings)) {
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
      transformed
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
