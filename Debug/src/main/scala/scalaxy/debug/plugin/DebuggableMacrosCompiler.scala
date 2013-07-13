// Author: Olivier Chafik (http://ochafik.com)
package scalaxy.debug.plugin
 
import scala.tools.nsc.CompilerCommand
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.ConsoleReporter
 
/**
 *  This compiler plugin adds source debug support to macro-expanded trees.
 *  
 *  It simply dumps the tree after the typer phase to some source file and labels tree nodes appropriately with line number info.
 */
object DebuggableMacrosCompiler {
  private val scalaLibraryJar =
    classOf[List[_]].getProtectionDomain.getCodeSource.getLocation.getFile
 
  def main(args: Array[String]) {
    try {
      val settings = new Settings
      val command = 
        new CompilerCommand(List("-bootclasspath", scalaLibraryJar) ++ args, settings)
 
      if (!command.ok)
        System.exit(1)
 
      val global = new Global(settings, new ConsoleReporter(settings)) {
        override protected def computeInternalPhases() {
          super.computeInternalPhases
          phasesSet += new DebuggableMacrosComponent(this)
        }
      }
      new global.Run().compile(command.files)
    } catch { 
      case ex: Throwable =>
        ex.printStackTrace
        System.exit(2)
    }
  }
}
