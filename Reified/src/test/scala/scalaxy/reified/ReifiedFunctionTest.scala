package scalaxy.reified.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror

import scalaxy.reified._

class ReifiedFunctionTest extends TestUtils {

  @Test
  def testDummyFunction {
    try {
      val r = reified((v: Int) => v * v)
      assertEquals("((v: Int) => v.$times(v))", r.flatExpr.tree.toString)
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
      val r = reified((v: Int) => x * v * y)
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
    def compose(capture1: Int): ReifiedValue[Int => Int] = {
      val capture2 = 666
      val f = reified((x: Int) => x * x + capture2)
      val capture3 = 1234
      val g = reified((x: Int) => capture1 + capture3)

      val comp = f.compose(g)
      assertTrue(comp.isInstanceOf[ReifiedFunction1[_, _]])
      comp
    }

    val comp10 = compose(10)
    //assertEquals(Seq(666, 10, 1234), comp10.flatten().capturedValues)
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
      val f = reified((x: Int) => capture1 + capture2(x))
      val g = reified((x: Int) => x * x)

      g.compose(f)
    }
    val comp10 = test(10)
    //println(comp10)
    assertSameEvals(comp10, 0, 1, 2)

    val comp100 = test(100)
    assertSameEvals(comp100, 0, 1, 2)
  }

}
