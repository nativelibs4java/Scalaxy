package scalaxy.js
package compiler

import scala.tools.nsc._
import scala.tools.nsc.transform.{ Transform, TypingTransformers }
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.reporters.Reporter

import java.io._

class CrossCompiler(outputDir: File, settings: Settings, reporter: Reporter)
    extends Global(settings, reporter) {

  addToPhasesSet(
    new CrossCompilationComponent(this, outputDir),
    "Converts Scala to JavaScript")

  //, JSLinkingComponent(this) if closure "linking" is enabled
  //, new JSExportComponent(this) if full compilation + export is enabled
}

object CrossCompiler { val args: Array[String] = Array()//extends App {

  def findClassPath(c: Class[_]): Option[String] =
    Option(c.getProtectionDomain.getCodeSource).map(_.getLocation.getFile)

  lazy val scalaLibraryJar = findClassPath(classOf[List[_]])
  lazy val scalaxyClassPath = findClassPath(classOf[scalaxy.js.global]).get

  var foundClassPathInArgs = false
  var outputDir = new File("target/javascript")
  def processArgs(args: List[String]): List[String] = args match {
    case "-o" :: o :: rest =>
      outputDir = new File(o)
      processArgs(rest)
    case "-cp" :: cp :: rest =>
      foundClassPathInArgs = true
      "-cp" :: Seq(cp, scalaxyClassPath).mkString(File.pathSeparator) :: processArgs(rest)
    case h :: rest =>
      h :: processArgs(rest)
    case Nil =>
      Nil
  }
  var compilerArgs = processArgs(args.toList)
  compilerArgs ++= List("-Yskip:" + CrossCompilationComponent.disablesPhases.mkString(","))
  if (!foundClassPathInArgs) {
    compilerArgs ++= List("-cp", scalaxyClassPath)
  }
  for (j <- scalaLibraryJar) {
    compilerArgs ++= List("-bootclasspath", j)
  }

  println("ARGS: " + compilerArgs.map(a => if (a.contains(" ")) "\"" + a + "\"" else a).mkString(" "))
  val settings = new Settings()
  val command = new CompilerCommand(compilerArgs, settings)
  if (command.ok) {
    val compiler = new CrossCompiler(outputDir, settings, new ConsoleReporter(settings))
    val run = new compiler.Run
    run.compile(command.files)
  }
}
