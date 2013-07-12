package scalaxy.reified.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror
import scala.tools.reflect.ToolBox

import scalaxy.reified._

class ReifiedFunctionTest extends TestUtils {

  @Test
  def testFunction {
    try {
      val x = 10
      val y = 20
      val r = reify((v: Int) => x * v * y)
      assertEquals(Seq(10, 20), r.capturedValues)
      assertEquals("((v: Int) => 10.*(v).*(20))", r.expr().tree.toString)
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
    assertEquals(Seq(10, 1234, 666), comp10.capturedValues)

    val comp100 = compose(100)
    assertEquals(Seq(100, 1234, 666), comp100.capturedValues)

    //println(comp10.expr().tree)
    //println(comp100.expr().tree)
  }

}
