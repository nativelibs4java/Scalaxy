package scalaxy.reified.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror
import scala.tools.reflect.ToolBox
import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag

import scalaxy.reified.{ reify, ReifiedValue }

trait TestUtils {

  lazy val toolbox = currentMirror.mkToolBox()

  def eval[A](r: ReifiedValue[A]): A = {
    eval(r.expr().tree).asInstanceOf[A]
  }

  def eval(tree: universe.Tree): Any = {
    try {
      toolbox.eval(tree)
    } catch {
      case _: Throwable =>
        //val reset = toolbox.resetAllAttrs(tree)
        val reset = toolbox.resetLocalAttrs(tree)
        try {
          toolbox.eval(reset)
        } catch {
          case th: Throwable =>
            println(s"Evaluation failure: " + reset)
            throw th
        }
    }
  }

  def assertSameEvals[A: TypeTag, B: TypeTag](f: ReifiedValue[A => B], inputs: A*) {
    //val toolbox = currentMirror.mkToolBox()
    for (input <- inputs) {
      val directEval = f(input)
      val reifiedF = eval(f)
      val reifiedEval = reifiedF(input)

      assertEquals(directEval, reifiedEval)
    }
  }
}
