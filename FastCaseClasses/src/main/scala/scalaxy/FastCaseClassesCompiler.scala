// Author: Olivier Chafik (http://ochafik.com)
package scalaxy.fastcaseclasses

import scala.tools.nsc.CompilerCommand
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.ConsoleReporter

/**
 *  This compiler plugin creates faster case classes.
 */
object FastCaseClassesCompiler {
  def jarOf(c: Class[_]) =
    Option(c.getProtectionDomain.getCodeSource).map(_.getLocation.getFile)
  val scalaLibraryJar = jarOf(classOf[List[_]])

  def main(args: Array[String]) {
    // new Thread() {
    //   override def run {
    //     Thread.sleep(20000)
    //     println("Took too long, killing the compiler process!")
    //     System.exit(1)
    //   }
    // }.start()
    try {
      val settings = new Settings
      val command =
        new CompilerCommand(
          scalaLibraryJar.map(jar => List("-bootclasspath", jar)).getOrElse(Nil) ++ args, settings)

      if (!command.ok)
        System.exit(1)

      val global = new Global(settings, new ConsoleReporter(settings)) {
        override protected def computeInternalPhases() {
          super.computeInternalPhases
          phasesSet += new TypedFastCaseClassesComponent(this)
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
