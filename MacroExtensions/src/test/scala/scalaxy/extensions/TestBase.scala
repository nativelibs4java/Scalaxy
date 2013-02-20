package scalaxy.extensions
package test

import java.io._

import scala.tools.nsc.CompilerCommand
import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.ConsoleReporter

trait TestBase {

  def transform(code: String): (String, String) = {
    val settings = new Settings
    val file = File.createTempFile("test", ".scala")
    file.deleteOnExit()
    try {
      val out = new PrintWriter(file)
      out.print(code)
      out.close()
      
      val args = Array(file.toString)
      val command = 
        new CompilerCommand(MacroExtensionsCompiler.scalaLibraryJar.map(jar => List("-bootclasspath", jar)).getOrElse(Nil) ++ args, settings)
  
      require(command.ok)
      
      var transformed: (String, String) = null
      val global = new Global(settings, new ConsoleReporter(settings)) {
        override protected def computeInternalPhases() {
          super.computeInternalPhases
          val comp = new MacroExtensionsComponent(this)
          phasesSet += comp
          phasesSet += new TestComponent(this, comp, (s, n) => transformed = (s, n))
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
    override val runsBefore = after.runsBefore
  
    def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {
      def apply(unit: CompilationUnit) {
        out(unit.body.toString, nodeToString(unit.body))
        unit.body = EmptyTree
      }
    }
  }
}
