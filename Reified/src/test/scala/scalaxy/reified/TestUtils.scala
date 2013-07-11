package scalaxy.reified.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror
import scala.tools.reflect.ToolBox

import scalaxy.reified.{ reify, ReifiedValue }

trait TestUtils {

  lazy val toolbox = currentMirror.mkToolBox()

  def eval(tree: universe.Tree): Any = {
    try {
      toolbox.eval(tree)
    } catch {
      case _: Throwable =>
        val reset = toolbox.resetAllAttrs(tree)
        try {
          toolbox.eval(reset)
        } catch {
          case th: Throwable =>
            println(s"Evaluation failure: " + reset)
            throw th
        }
    }
  }
  def assertSameEvals[A, B](f: ReifiedValue[A => B], inputs: A*) {
    //val toolbox = currentMirror.mkToolBox()
    for (input <- inputs) {
      val directEval = f(input)

      val tree = f.expr().tree
      val reifiedEval = toolbox.eval(tree)

      assertEquals(directEval, reifiedEval)
    }
  }
}
