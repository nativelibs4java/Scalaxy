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
  // with Transform
  // with TypingTransformers
  with ScalaToJavaScriptConversion {

  val phaseName = "javascript"
  val runsRightAfter = Option("mixin")
  val runsAfter = runsRightAfter.toList
  override val runsBefore = List("cleanup")

  import global._

  println("Created CrossCompilationComponent")
  def newPhase(prev: Phase) = new StdPhase(prev) {
    println("Created phase")
    def apply(unit: CompilationUnit) {
      println("APPLY!!!")
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
