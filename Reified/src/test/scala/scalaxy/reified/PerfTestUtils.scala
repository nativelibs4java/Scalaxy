package scalaxy.reified.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror

import scalaxy.reified._

trait PerfTestUtils {

  def benef(normal: Double, recompiled: Double): String = {
    val factor = twoDecimals(normal / recompiled)
    val sign = if (normal > recompiled) "-" else "+"
    val percent = math.abs(oneDecimal((recompiled - normal) / normal * 100))

    s"$factor x ($sign $percent %)"
  }

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
  def nanoTime[V](body: => V): (V, Double) = {
    val start = System.nanoTime
    val v = body
    val res = (System.nanoTime - start)
    (v, res)
  }

  def nanoTimeAvg(n: Int, title: String)(body: => Unit): Double = {
    val (_, t) = nanoTime {
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

  def compare(title: String, n: Int, its: Int)(r: ReifiedValue[() => Unit]) {
    val f = r.value
    val (compiledF, compilationTime) = nanoTime { r.compile()() }

    val pref = s"[$title] "
    println(pref + s"compilation: ${formatNanos(compilationTime)}")
    //nanoTimeAvg(100, pref + "compilation") { r.compile()() }
    println()

    assertEquals("Mismatching results", f(), compiledF())

    val results = for (i <- 0 until its) yield {
      val normal = nanoTimeAvg(n, pref + "normal")(f())
      val recompiled = nanoTimeAvg(n, pref + "recompiled")(compiledF())
      println(pref + "benefit: " + benef(normal, recompiled))
      println()
      (normal, recompiled)
    }
    val (normal, recompiled) =
      results.unzip
    //results.drop(1).unzip
    val normalAvg = normal.sum / normal.size.toFloat * n
    val recompiledAvg = recompiled.sum / normal.size.toFloat * n

    println(pref + "TOTAL:")
    println(pref + "NORMAL avg: " + formatNanos(normalAvg))
    println(pref + "RECOMPILED avg: " + formatNanos(recompiledAvg))
    println(pref + "BENEFIT avg: " + benef(normalAvg, recompiledAvg))
    println()
  }

}
