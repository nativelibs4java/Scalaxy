// Author: Olivier Chafik (http://ochafik.com)
package scalaxy.streams

import scala.tools.nsc.CompilerCommand
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.reporters.{ ConsoleReporter, Reporter }

/**
 *  This modified compiler enforces a "default-private" semantics, with a `@public` annotation to mark entities as public.
 */
object StreamsCompiler {
  def jarOf(c: Class[_]) =
    Option(c.getProtectionDomain.getCodeSource).map(_.getLocation.getFile)
  val scalaLibraryJar = jarOf(classOf[List[_]])

  def main(args: Array[String]) {
    compile(
      args,
      settings => new ConsoleReporter(settings),
      StreamsPlugin.getInternalPhases _)
  }

  def compile[R <: Reporter](args: Array[String],
    reporterGetter: Settings => R,
    internalPhasesGetter: Global => List[PluginComponent]): R = {
    try {
      val settings = new Settings
      val command =
        new CompilerCommand(
          scalaLibraryJar.map(jar => List("-bootclasspath", jar)).getOrElse(Nil) ++ args, settings)

      if (!command.ok)
        System.exit(1)

      val reporter = reporterGetter(settings)
      val global = new Global(settings, reporter) {
        override protected def computeInternalPhases() {
          super.computeInternalPhases
          phasesSet ++= internalPhasesGetter(this)
        }
      }
      new global.Run().compile(command.files)

      reporter
    } catch {
      case ex: Throwable =>
        ex.printStackTrace
        System.exit(2)
        throw ex
    }
  }
}
