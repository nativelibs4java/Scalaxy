package scalaxy.reified.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror
import scala.tools.reflect.ToolBox

import scalaxy.reified._

class ReifiedTest extends TestUtils {

  @Test
  def testValue {
    val x = 10
    val r = reify(100 * x)
    assertTrue(r.isInstanceOf[ReifiedValue[_]])
    assertEquals(Seq(10), r.capturedValues)
    assertEquals("100.*(10)", r.expr().tree.toString)
  }

  @Test
  def testFunction {
    val x = 10
    val y = 20
    val r = reify((v: Int) => x * v * y)
    val f: Int => Int = r
    assertTrue(f.isInstanceOf[ReifiedFunction1[_, _]])
    assertEquals(Seq(10, 20), r.capturedValues)
    assertEquals("((v: Int) => 10.*(v).*(20))", r.expr().tree.toString)
  }

  @Test
  def testImplicitValueConversion {
    import scala.language.implicitConversions

    val v = 2
    val r = reify(10 + v)
    val i: Int = r
    assertEquals(12, i)
  }

  @Test
  def testImplicitFunctionConversion {
    import scala.language.implicitConversions

    val v = 2
    val r = reify((x: Int) => x + v)
    val f: Int => Int = r
    assertEquals(12, f(10))
  }

  @Ignore
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

  @Ignore
  @Test
  def testCapture2 {
    def test(capture1: Int): ReifiedFunction1[Int, Int] = {
      // Capture of arrays is TODO
      val capture2 = Seq(10, 20, 30)
      val f = reify((x: Int) => capture1 + capture2(x))
      val g = reify((x: Int) => x * x)

      g.compose(f)
    }

    println(test(10).expr().tree)
    println(test(100).expr().tree)
  }

}
