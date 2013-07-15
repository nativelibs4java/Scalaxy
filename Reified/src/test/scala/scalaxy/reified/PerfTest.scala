package scalaxy.reified.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror

import scalaxy.reified._

class PerfTest extends TestUtils {

  def time(n: Int, title: String)(body: => Unit): Double = {
    val start = System.nanoTime
    var i = 0
    while (i < n) {
      body
      i += 1
    }
    val res = (System.nanoTime - start) / 1000000.0
    println(title + ": " + res)
    res
  }

  @Test
  def testComposite3 {

    def test(capture1: Int) = {
      val capture2 = Array(10, 20, 30)
      val f = reify((x: Int) => capture1 + capture2(x))
      val g = reify((x: Int) => x * x)
      val h = reify((x: Int) => -x)

      g.compose(f).andThen(h)
    }
    val n = 1000000
    val its = 10

    val comp10 = test(10)

    val f = comp10.value
    val F = comp10.compile()()

    val results = for (i <- 0 until its) yield {
      (
        time(n, "NORMAL")(f(0) + f(1) + f(2)),
        time(n, "RECOMPILED")(F(0) + F(1) + F(2))
      )
    }
    println()
    val (normal, recompiled) =
      results.unzip
    //results.drop(1).unzip

    println("NORMAL average: " + (normal.sum / normal.size.toFloat))
    println("RECOMPILED average: " + (recompiled.sum / normal.size.toFloat))
  }

}
