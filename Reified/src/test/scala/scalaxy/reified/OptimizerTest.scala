package scalaxy.reified.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror

import scalaxy.reified._

@Ignore
class OptimizerTest extends TestUtils {

  @Test
  def testRange {
    val f = reified((i: Int) => i * scala.math.cos(i * scala.math.Pi / 12.0))
    val r = reified((v: Int) => {
      var tot = 0.0
      for (i <- 0 until v) {
        tot += i * f(i)
      }
      tot
    })
    //assertEquals("((v: Int) => v.*(v))", r.expr().tree.toString)
    assertSameEvals(r, -1, 0, 1, 100)

  }
}
