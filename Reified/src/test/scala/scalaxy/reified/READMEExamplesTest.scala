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
class READMEExamplesTest extends TestUtils {
  
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
  def testReifiedRange {
    import scalaxy.reified._
    def createDiscreteIntegrator(f: ReifiedValue[Double => Double]): ReifiedValue[(Int, Int) => Double] = {
      reify((start: Int, end: Int) => {
        var tot = 0.0
        for (i <- start until end) {
          tot += f(i)
        }
        tot
      })
    }
    import scala.math._
    val fIntegrator = createDiscreteIntegrator(reify(v => {
      cos(v / Pi) * exp(v)
    }))
    
    val f = fIntegrator
    println(f.taggedExpr)
    println(f.capturedTerms)
    
    val ff = f.compile()()
    for ((start, end) <- Seq((0, 1), (0, 10))) {
      assert(f(start, end) == ff(start, end))
    }
    
    println(fIntegrator(0, 1))
    println(fIntegrator(0, 10))
  }
