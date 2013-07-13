package scalaxy.reified.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror
import scala.tools.reflect.ToolBox

import scalaxy.reified._

class ReifiedFunctionTest extends TestUtils {

  @Test
  def testDummyFunction {
    try {
      val r = reify((v: Int) => v * v)
      assertEquals("((v: Int) => v.*(v))", r.expr().tree.toString)
      assertSameEvals(r, 0, 1, 2)
    } catch {
      case th: Throwable =>
        th.printStackTrace(System.out)
        throw th
    }
  }

  @Test
  def testFunctionWithCaptures {
    try {
      val x = 10
      val y = 20
      val r = reify((v: Int) => x * v * y)
      assertEquals(Seq(10, 20), r.capturedValues)
      assertSameEvals(r, 0, 1, 2)
      //assertEquals("((v: Int) => 10.*(v).*(20))", r.expr().tree.toString)
    } catch {
      case th: Throwable =>
        th.printStackTrace(System.out)
        throw th
    }
  }

  @Test
  def testFunctionComposition {
    def compose(capture1: Int): ReifiedFunction1[Int, Int] = {
      val capture2 = 666
      val f = reify((x: Int) => x * x + capture2)
      val capture3 = 1234
      val g = reify((x: Int) => capture1 + capture3)

      f.compose(g)
    }

    val comp10 = compose(10)
    //assertEquals(Seq(10, 1234, 666), comp10.capturedValues)
    assertSameEvals(comp10, -1, 0, 1, 2)

    val comp100 = compose(100)
    //assertEquals(Seq(100, 1234, 666), comp100.capturedValues)
    assertSameEvals(comp100, -1, 0, 1, 2)

    //println(comp10.expr().tree)
    //println(comp100.expr().tree)
  }

  @Test
  def testCapture2 {
    def test(capture1: Int) = {
      val capture2 = Array(10, 20, 30)
      val f = reify((x: Int) => capture1 + capture2(x))
      val g = reify((x: Int) => x * x)

      g.compose(f)
    }
    val comp10 = test(10)
    println(comp10)
    assertSameEvals(comp10, 0, 1, 2)

    val comp100 = test(100)
    assertSameEvals(comp100, 0, 1, 2)
  }

}
