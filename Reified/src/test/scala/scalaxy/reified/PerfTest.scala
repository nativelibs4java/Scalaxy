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
    twoDecimals(normal / recompiled)

  def oneDecimal(v: Double): Double = (v * 10).toInt / 10.0
  def twoDecimals(v: Double): Double = (v * 100).toInt / 100.0

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

  def comp2(capture1: Int) = {
    val capture2 = Array(10, 20, 30)
    val f = reify((x: Int) => capture1 + capture2(x))
    val g = reify((x: Int) => x * x)
    g.compose(f)
  }

  @Test
  def testComposite2 {
    val n = 1000000
    val its = 4

    val f = comp2(10)
    compare("testComposite2", n, its)(reify(() => f(0) + f(1) + f(2)))
  }

  @Test
  def testComposite3 {
    val n = 1000000
    val its = 4

    val f = comp2(10).andThen(reify((x: Int) => -x))
    compare("testComposite3", n, its)(reify(() => f(0) + f(1) + f(2)))
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
    val its = 4

    compare("testRanges", n, its)(reify(() => f(100) - f(10)))
  }

  def compare(title: String, n: Int, its: Int)(r: ReifiedValue[() => Unit]) {
    val f = r.value
    val (compiledF, compilationTime) = time1 { r.compile()() }

    val pref = s"[$title] "
    //println(s"First Compilation Time = $compilationTime ms")
    times(100, pref + "compilation") { r.compile()() }
    println()

    assertEquals("Mismatching results", f(), compiledF())

    val results = for (i <- 0 until its) yield {
      val normal = times(n, pref + "normal")(f())
      val recompiled = times(n, pref + "recompiled")(compiledF())
      println(pref + "benefit: " + benef(normal, recompiled) + " x")
      println()
      (normal, recompiled)
    }
    val (normal, recompiled) =
      results.unzip
    //results.drop(1).unzip
    val normalAvg = normal.sum / normal.size.toFloat * n
    val recompiledAvg = recompiled.sum / normal.size.toFloat * n

    println(pref + "TOTAL:")
    println(pref + "normal avg: " + formatNanos(normalAvg))
    println(pref + "recompiled avg: " + formatNanos(recompiledAvg))
    println(pref + "benefit avg: " + benef(normalAvg, recompiledAvg) + " x")
    println()
  }

}
