package scalaxy.reified.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.TypeTag
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
      val getter = reified((index: Int) => offset + values(index))
      val square = reified((x: Int) => x * x)
      square.compose(getter)
    }
    val f: ReifiedValue[Int => Int] = comp(10)
    //println(f.taggedExpr)
    //println(f.capturedTerms)

    val ff = f.compile()()
    for (index <- Seq(0, 1, 2)) {
      assert(f(index) == ff(index))
    }
  }

  import scalaxy.reified._
  def createDiscreteIntegrator2D(f: ReifiedValue[(Double, Double) => Double], step: Double): ReifiedValue[(Double, Double, Double, Double) => Double] = {
    (xMin: Double, xMax: Double, yMin: Double, yMax: Double) =>
      {
        val nx = ((xMax - xMin) / step).toInt
        val ny = ((yMax - yMin) / step).toInt

        var sum = 0.0
        val halfStep = step / 2
        var x = xMin + halfStep
        var y = yMin + halfStep
        for (i <- 0 to nx) {
          for (j <- 0 to ny) {
            sum += f(x, y)
            y += step
          }
          x += step
        }
        step * sum
      }
  }
  def createDiscreteIntegrator1D(f: ReifiedValue[Double => Double], step: Double): ReifiedValue[(Double, Double) => Double] = {
    (xMin: Double, xMax: Double) =>
      {
        val nx = ((xMax - xMin) / step).toInt

        var sum = 0.0
        var x = xMin + step / 2
        for (i <- 0 to nx) {
          sum += f(x)
          x += step
        }
        step * sum
      }
  }

  @Test
  def testDiscreteIntegrator2D {

    import scala.math._

    val factor = 2.0
    val f = reified((x: Double, y: Double) => {
      cos(x * factor) - sin(y / factor)
    })
    val fIntegrator = createDiscreteIntegrator2D(f, 0.1)
    //val fIntegrator = createDiscreteIntegrator2D((v: Double) => f(shift(v)))
    //val fIntegrator = createDiscreteIntegrator2D(f)

    //println("fIntegrator.taggedExpr = " + fIntegrator.taggedExpr)
    //println("fIntegrator.capturedTerms = " + fIntegrator.capturedTerms)

    val fasterIntegrator = fIntegrator.compile()()
    val areas = Seq(((0, 1), (0, 1)), ((5, 6), (2, 3)))
    for (((xMin, xMax), (yMin, yMax)) <- areas) {
      assert(fIntegrator(xMin, xMax, yMin, yMax) == fasterIntegrator(xMin, xMax, yMin, yMax))
    }

    //println(fIntegrator(0, 1))
    //println(fIntegrator(0, 10))

    val n = 3000
    val iterations = 3

    compare("testDiscreteIntegrator", n, iterations)(reified(() => {
      fIntegrator(0, 1, 0, 1) + fIntegrator(2, 5, 3, 4)
    }))
  }

  import scalaxy.reified._
  def createDiscreteConvolver1D(
    f: ReifiedValue[Double => Double],
    g: ReifiedValue[Double => Double],
    step: Double = 0.1): ReifiedValue[(Double, Double) => Double] = {

    (xMin: Double, xMax: Double) =>
      {
        var sum = 0.0
        val iMin = (xMin / step).toInt
        val iMax = (xMax / step).toInt
        for (i <- iMin until iMax) {
          val x = i * step;
          sum += f(x) * g(xMax - (x - xMin))
        }
        step * sum
      }
  }

  @Test
  def testDiscreteConvolver2D {

    import scala.math._

    val factor = 1 / Pi
    val f = reified(cos(_))
    val g = reified(sin(_))
    val fgConvolver = createDiscreteConvolver1D(f, g)

    //println("fgConvolver.taggedExpr = " + fgConvolver.taggedExpr)
    //println("fgConvolver.capturedTerms = " + fgConvolver.capturedTerms)

    val fasterConvolver = fgConvolver.compile()()
    for ((start, end) <- Seq((0, 1), (0, 10))) {
      assert(fgConvolver(start, end) == fgConvolver(start, end))
    }

    println(fgConvolver(0, 1))
    println(fgConvolver(0, 10))

    val n = 3000
    val iterations = 3

    compare("testDiscreteConvolver", n, iterations)(reified(() => {
      fgConvolver(0, 10) + fgConvolver(20, 30) + fgConvolver(30, 40)
    }))
  }

  def assertEqualsRelativePrecision(expected: Double, actual: Double, relativePrecision: Double) {
    assertEquals(expected, actual, Math.abs(expected * relativePrecision))
  }

  @Test
  def poly {
    val f = reified((x: Double) => 1 + x * (2 + x * (3 + x * 2))) // 1 + 2x + 3x^2 + 2x^3
    val fDerivate = reified((x: Double) => 2 + x * (6 + x * 6))
    val fPrimitive = (x: Double) => x * (1 + x * (2 + x * (1 + x * 1 / 2.0)))

    val xMin = 0
    val xMax = 10
    val step = 0.01
    val relativePrecision = 0.02

    val fIntegrator = createDiscreteIntegrator1D(f, step)
    val fCompiledIntegrator = fIntegrator.compile()()

    // Check integral
    assertEqualsRelativePrecision(
      fPrimitive(xMax) - fPrimitive(xMin),
      fCompiledIntegrator(xMin, xMax),
      relativePrecision)

    // Integrate f derivative to check
    assertEqualsRelativePrecision(
      f(xMax) - f(xMin),
      createDiscreteIntegrator1D(fDerivate, step).compile()()(xMin, xMax),
      relativePrecision)

    // Compare performance
    val n = 3000
    val iterations = 3

    compare("poly", n, iterations)(reified(() => {
      fIntegrator(0, 10) + fIntegrator(20, 30) + fIntegrator(30, 40)
    }))
  }
}
