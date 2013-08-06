package scalaxy.generic.trees.test

import scalaxy.generic._
import scalaxy.generic.trees._

import org.junit._
import org.junit.Assert._

// import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.TypeTag
// import scala.reflect.runtime.currentMirror

import scalaxy.generic._
import scala.language.implicitConversions
// import scala.language.dynamics

class GenericTest {
  class A(var a: Int) {
    def square1 = a * a
    def square2() = a * a
    def mul(y: Int) = a * y
    def y = a
  }
  @Test
  def testGenericClass {
    val value = generic(new A(10))
    assertEquals(100, value.square1)
    assertEquals(100, value.square2)
    assertEquals(20, value.mul(2))
    assertEquals(10, value.a)
    assertEquals(10, value.y)
  }

  @Ignore
  @Test
  def testUpdate {
    val value = generic(new A(10))
    // TODO: Fix compilation of this ("macros cannot be partially applied"):
    // value.a = 11
    // assertEquals(11, value.a)
  }

  @Test
  def testNumerics {
    val a = generic(10)
    assertEquals(12, a + 2)
    assertEquals(8, a - 2)
    assertEquals(20, a * 2)
    assertEquals(5, a / 2)
    assertEquals(10.0, a.toDouble, 0)
  }

  def generic[A: Generic](value: A) = new GenericOps[A](value)

  @Test
  def testAlternatives {
    def test[N: TypeTag: Generic](a: N) {
      assertEquals(12, a.toInt)
    }
    test(generic(12: Byte))
    test(generic(12: Short))
    test(generic(12))
    test(generic(12L))
    test(generic(12.0))
    test(generic(12.0f))

    // TODO
  }

  def genericLimit[N: Generic](target: N, f: N => N): Int = {
    var v = one[N]
    var n = 0
    while (v < target) {
      v = f(v) * number[N](3) / number[N](2)
      n += 1
    }
    n
  }

  @Test
  def testGenericLimit {
    assertEquals(3, genericLimit[Int](10, _ * 2))
    assertEquals(2, genericLimit[Int](10, _ * 3))
    assertEquals(2, genericLimit[Double](10.0, _ * 3.0))
  }
}
