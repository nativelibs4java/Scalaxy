package scalaxy.js
package compiler

import scala.tools.nsc._
import scala.tools.nsc.transform.{ Transform, TypingTransformers }
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.reporters.Reporter

import java.io.File

object CrossCompilationComponent {
  val disablesPhases = List("lazyvals")
}
class CrossCompilationComponent(val global: Global, val outputDir: File)
  extends SubComponent
  with ScalaToJavaScriptConversion {

  val phaseName = "javascript"
  val runsRightAfter = Option("flatten")
  val runsAfter = runsRightAfter.toList
  override val runsBefore = List("mixin")

  import global._

  def newPhase(prev: Phase) = new StdPhase(prev) {
    def apply(unit: CompilationUnit) {
      val javaScriptCode = convert(unit.body)
      println("CONVERTED TO JavaScript:\n" + javaScriptCode)
      write(
        javaScriptCode,
        new File(outputDir,
          new File(unit.source.path.toString)
            .getName.replaceAll("(.*?)\\.scala", "$1") + ".js"))

      unit.body = EmptyTree
    }
  }
}
