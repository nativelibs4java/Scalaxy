package scalaxy.reified.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror

import scalaxy.reified._

class SimpleTest extends TestUtils {

  @Test
  def test {
    val x = 10
    val f = reified((x: Int) => x + 1)
    val g = reified((x: Int) => f(x))

    println(g.compile()()(10))
    // val a = reified(100 * x)
    // val b = reified(100 * a)
    // assertTrue(b.isInstanceOf[Reified[_]])
    // assertEquals(Seq(10), a.capturedValues)
    // assertEquals(Seq(a), b.capturedValues)
    // //assertEquals("100.*(10)", r.expr().tree.toString)
    // assertEquals(100 * 100 * 10, b.compile()(), 0)
  }
}
