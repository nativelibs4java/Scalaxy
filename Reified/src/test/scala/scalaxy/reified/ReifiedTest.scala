package scalaxy.reified.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror
import scala.reflect.runtime.universe.TypeTag

import scalaxy.reified._
import scalaxy.reified.internal.Optimizer

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

  import scalaxy.generic.Generic

  // case class GenericPolynomial[A: Generic: TypeTag](coefficients: A*) {
  //   import scalaxy.generic._
  //   def computeGenericPolynomial(coefficients: List[A]): ReifiedValue[A => A] = coefficients match {
  //     case Nil =>
  //       reify((x: A) => zero[A])
  //     case c :: others =>
  //       val sub = computeGenericPolynomial(others)
  //       reify((x: A) => c + x * sub(x))
  //   }
  //   lazy val function: ReifiedValue[A => A] = computeGenericPolynomial(coefficients.toList)
  // }

  def computeNumericPolynomial[A: Numeric: TypeTag](coefficients: List[A]): ReifiedValue[A => A] = coefficients match {
    case Nil =>
      reify((x: A) => implicitly[Numeric[A]].zero)
    case c :: others =>
      import Numeric.Implicits._
      val sub = computeNumericPolynomial(others)
      reify((x: A) => c + x * sub(x))
  }
  case class NumericPolynomial[A: Numeric: TypeTag](coefficients: A*) {
    lazy val function: ReifiedValue[A => A] = computeNumericPolynomial(coefficients.toList)
  }

  // @Ignore
  // @Test
  // def testGenericPolynomial {
  //   val p = GenericPolynomial[Double](1, 2, 3, 4)
  //   val r = p.function
  //   val f = r.value
  //   val fc = r.compile()()
  //   for (x <- 0 until 10) {
  //     assertEquals(f(x), fc(x))
  //   }
  // }

  @Test
  def testNumericPolynomial {
    val p = NumericPolynomial[Double](1, 2, 3, 4)
    val r = p.function
    val f = r.value
    println("testNumericPolynomial: " + Optimizer.optimize(r.expr()._1.tree))
    val fc = r.compile()()
    for (x <- 0 until 10) {
      assertEquals(f(x), fc(x), 0)
    }
  }

}
