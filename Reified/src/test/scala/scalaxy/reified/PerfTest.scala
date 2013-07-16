package scalaxy.reified.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror

import scalaxy.reified._

class PerfTest extends TestUtils {

  def time1[V](body: => V): (V, Double) = {
    val start = System.nanoTime
    val v = body
    val res = (System.nanoTime - start)
    (v, res)
  }

  def benef(normal: Double, recompiled: Double): Double =
    oneDecimal(normal / recompiled)

  def oneDecimal(v: Double): Double = (v * 10).toInt / 10.0

  def formatNanos(n: Double): String = {
    val (v, u) =
      if (n < 1000) n -> "nanosec"
      else if (n < 1000000) n / 1000 -> "microsec"
      else if (n < 1000000000) n / 1000000 -> "millisec"
      else n / 1000000000 -> "sec"
    oneDecimal(v) + " " + u
  }
  def times(n: Int, title: String)(body: => Unit): Double = {
    val (_, t) = time1 {
      var i = 0
      while (i < n) {
        body
        i += 1
      }
    }
    val div = t / n.toDouble
    println(s"$title: ${formatNanos(div)}")
    div
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
    val (compiledF, compilationTime) = time1 { comp10.compile()() }

    //println(s"FirstCompilation Time = $compilationTime ms")
    times(100, "COMPILATION") { comp10.compile()() }

    val results = for (i <- 0 until its) yield {
      val normal = times(n, "NORMAL")(f(0) + f(1) + f(2))
      val recompiled = times(n, "RECOMPILED")(compiledF(0) + compiledF(1) + compiledF(2))
      println("BENEFIT: " + benef(normal, recompiled) + " x")
      println()
      (normal, recompiled)
    }
    println()
    val (normal, recompiled) =
      results.unzip
    //results.drop(1).unzip
    val normalAvg = normal.sum / normal.size.toFloat * n
    val recompiledAvg = recompiled.sum / normal.size.toFloat * n

    println("NORMAL average: " + formatNanos(normalAvg))
    println("RECOMPILED average: " + formatNanos(recompiledAvg))
    println("BENEFIT average: " + benef(normalAvg, recompiledAvg) + " x")

  }

}
