package scalaxy.reified.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror
import scala.reflect.runtime.universe.TypeTag

import scalaxy.reified._

class ReifiedTest extends TestUtils {

  // Conflicts with value -> function conversion?
  @Test
  def testImplicitValueConversion {
    import scala.language.implicitConversions

    val v = 2
    val r = reify(10 + v)
    val i: Int = r.value
    assertEquals(12, i)
  }

  @Test
  def testImplicitFunctionConversion {
    import scala.language.implicitConversions

    val v = 2
    val r = reify((x: Int) => x + v)

    val f: Int => Int = r.value
    val rf: ReifiedFunction1[Int, Int] = r
    val ff: Int => Int = rf.value

    val rr = r.compose(r)
    val rrf: ReifiedFunction1[Int, Int] = rr

    assertEquals(12, f(10))
  }

  import scalaxy.generic._

  def computePolynomial[A: Generic: TypeTag](coefficients: List[A]): ReifiedValue[A => A] = coefficients match {
    case Nil =>
      reify((x: A) => zero[A])
    case c :: others =>
      val sub = computePolynomial(others)
      reify((x: A) => c + x * sub(x))
  }
  case class Polynomial[A: Generic: TypeTag](coefficients: A*) {
    lazy val function: ReifiedValue[A => A] = computePolynomial(coefficients.toList)
  }

  @Ignore
  @Test
  def testPoly {
    val p = Polynomial[Double](1, 2, 3, 4)
    val r = p.function
    val f = r.value
    val fc = r.compile()()
    for (x <- 0 until 10) {
      assertEquals(f(x), fc(x))
    }
  }

}
