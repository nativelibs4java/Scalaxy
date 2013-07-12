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

  implicit class ReifiedValueExtensions[A](r: ReifiedValue[A]) {
    private[test] def capturedValues: Seq[AnyRef] = r.capturedTerms.map(_._1)
  }

  def assertSameEvals[A: TypeTag, B: TypeTag](f: ReifiedValue[A => B], inputs: A*) {
    //val toolbox = currentMirror.mkToolBox()
    for (input <- inputs) {
      val directEval = f(input)
      val reifiedF = f.compile()()
      val reifiedEval = reifiedF(input)

      assertEquals(directEval, reifiedEval)
    }
  }
}
