package scalaxy.reified.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror
import scala.tools.reflect.ToolBox

import scalaxy.reified._

class ReifiedTest extends TestUtils {

  // Conflicts with value -> function conversion?
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
    val rf: ReifiedFunction1[Int, Int] = r
    val ff: Int => Int = rf

    val rr = r.compose(r)
    val rrf: ReifiedFunction1[Int, Int] = rr

    assertEquals(12, f(10))
  }

  @Test
  def testCapture2 {
    def test(capture1: Int) = {
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
