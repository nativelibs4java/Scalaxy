package scalaxy.reified.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror

import scalaxy.reified._
import scalaxy.debug._

/**
 * Examples from the README
 */
class READMEExamplesTest extends TestUtils with PerfTestUtils {

  @Test
  def testReified {
    import scalaxy.reified._
    def comp(offset: Int) = {
      val values = Array(10, 20, 30)
      val getter = reify((index: Int) => offset + values(index))
      val square = reify((x: Int) => x * x)
      square.compose(getter)
    }
    val f: ReifiedValue[Int => Int] = comp(10)
    println(f.taggedExpr)
    println(f.capturedTerms)

    val ff = f.compile()()
    for (index <- Seq(0, 1, 2)) {
      assert(f(index) == ff(index))
    }
  }

  @Test
  def testDiscreteIntegrator {
    import scalaxy.reified._
    def createDiscreteIntegrator(f: ReifiedValue[(Double, Double) => Double], step: Double = 0.1): ReifiedValue[(Double, Double, Double, Double) => Double] = {
      (xMin: Double, xMax: Double, yMin: Double, yMax: Double) =>
        {
          var tot = 0.0
          val iMin = (xMin / step).toInt
          val iMax = (xMax / step).toInt
          val jMin = (yMin / step).toInt
          val jMax = (yMax / step).toInt
          for (i <- iMin until iMax) {
            val x = i * step;
            for (j <- jMin until jMax) {
              val y = j * step
              tot += f(x, y)
            }
          }
          tot
        }
    }
    import scala.math._

    val factor = 2.0
    val f = reify((x: Double, y: Double) => {
      cos(x * factor) - sin(y / factor)
    })
    val fIntegrator = createDiscreteIntegrator(f)
    //val fIntegrator = createDiscreteIntegrator((v: Double) => f(shift(v)))
    //val fIntegrator = createDiscreteIntegrator(f)

    println("fIntegrator.taggedExpr = " + fIntegrator.taggedExpr)
    println("fIntegrator.capturedTerms = " + fIntegrator.capturedTerms)

    val fasterIntegrator = fIntegrator.compile()()
    val areas = Seq(((0, 1), (0, 1)), ((5, 6), (2, 3)))
    for (((xMin, xMax), (yMin, yMax)) <- areas) {
      assert(fIntegrator(xMin, xMax, yMin, yMax) == fasterIntegrator(xMin, xMax, yMin, yMax))
    }

    //println(fIntegrator(0, 1))
    //println(fIntegrator(0, 10))

    val n = 1000
    val iterations = 3

    compare("testDiscreteIntegrator", n, iterations)(reify(() => {
      fIntegrator(0, 1, 0, 1) + fIntegrator(0, 10, 0, 10)
    }))
  }

  @Test
  def testDiscreteConvolver {
    import scalaxy.reified._
    def createDiscreteConvolver(
      f: ReifiedValue[Double => Double],
      g: ReifiedValue[Double => Double],
      step: Double = 0.1): ReifiedValue[(Double, Double) => Double] = {

      (xMin: Double, xMax: Double) =>
        {
          var tot = 0.0
          val iMin = (xMin / step).toInt
          val iMax = (xMax / step).toInt
          for (i <- iMin until iMax) {
            val x = i * step;
            tot += f(x) * g(xMax - (x - xMin))
          }
          tot
        }
    }
    import scala.math._

    val factor = 1 / Pi
    val f = reify(cos(_))
    val g = reify(sin(_))
    val fgConvolver = createDiscreteConvolver(f, g)

    println("fgConvolver.taggedExpr = " + fgConvolver.taggedExpr)
    println("fgConvolver.capturedTerms = " + fgConvolver.capturedTerms)

    val fasterConvolver = fgConvolver.compile()()
    for ((start, end) <- Seq((0, 1), (0, 10))) {
      assert(fgConvolver(start, end) == fgConvolver(start, end))
    }

    println(fgConvolver(0, 1))
    println(fgConvolver(0, 10))

    val n = 3000
    val iterations = 3

    compare("testDiscreteConvolver", n, iterations)(reify(() => {
      fgConvolver(0, 10) + fgConvolver(20, 30) + fgConvolver(30, 40)
    }))
  }
}
