package scalaxy.reified.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror

import scalaxy.reified._

object AlmostMath {
  class Sub {
    def almostCos(x: Double) = scala.math.cos(x) * 0.99
  }
  val sub = new Sub
}

class ReifiedValueTest extends TestUtils {

  @Test
  def testCaptureConstant {
    val x = 10
    val r = reify(100 * x)
    assertTrue(r.isInstanceOf[ReifiedValue[_]])
    assertEquals(Seq(10), r.capturedValues)
    //assertEquals("100.*(10)", r.expr().tree.toString)
    assertEquals(100 * 10, r.compile()(), 0)
  }

  @Test
  def testCaptureMap {
    val x = Map("a" -> 10, "b" -> 20)
    val r = reify((s: String) => 100 * x(s))
    assertSameEvals(r, "a", "b")
  }

  @Test
  def testCaptureMath {
    val x = 10
    import scala.math._
    import AlmostMath._
    val r = reify(100 * math.cos(x) * sub.almostCos(x * x))
    assertEquals(r.value, r.compile()(), 0)
  }

  @Ignore
  @Test
  def testFlat {
    val x = Seq(1, 2, 3)
    val y = 12 + " things"

    val a = reify(Seq(x, "blah"))
    val b = reify((y, a))

    assertEquals(Seq(x), a.asInstanceOf[ReifiedValue[_]].capturedValues)
    assertEquals(Seq(y, a), b.capturedValues)

    assertEquals(b.value, b.compile()())
  }
}
