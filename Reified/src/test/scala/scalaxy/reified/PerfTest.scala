package scalaxy.reified.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror

import scalaxy.reified._

//@Ignore
class PerfTest extends TestUtils with PerfTestUtils {

  def comp2(capture1: Int) = {
    val capture2 = Array(10, 20, 30)
    val f = reify((x: Int) => capture1 + capture2(x))
    val g = reify((x: Int) => x * x)
    g.compose(f)
  }

  @Test
  def testComposite2 {
    val n = 1000000
    val iterations = 4

    val f = comp2(10)
    compare("testComposite2", n, iterations)(reify(() => f(0) + f(1) + f(2)))
  }

  @Test
  def testComposite3 {
    val n = 1000000
    val iterations = 4

    val f = comp2(10).andThen(reify((x: Int) => -x))
    compare("testComposite3", n, iterations)(reify(() => f(0) + f(1) + f(2)))
  }

  @Test
  def testRanges {
    val g = reify((x: Double) => {
      x * math.cos(x)
    })
    val f = reify((n: Int) => {
      var tot = 0.0
      val n2 = n * n
      for (i <- 0 until n) {
        tot += (i * i + g(i)) / n2
      }
      tot
    })

    val n = 10000
    val iterations = 4

    compare("testRanges", n, iterations)(reify(() => f(100) - f(10)))
  }
}

