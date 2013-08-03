package scalaxy.reified.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.TypeTag
import scala.reflect.runtime.currentMirror

import scalaxy.reified._
import scalaxy.debug._
import scala.language.implicitConversions
import scala.language.dynamics

class GenericTest extends TestUtils {
  class A(var a: Int) {
    def square1 = a * a
    def square2() = a * a
    def mul(y: Int) = a * y
    def y = a
  }
  @Test
  def testGenericClass {
    val value = Generic(new A(10))
    assertEquals(100, value.square1)
    assertEquals(100, value.square2)
    assertEquals(20, value.mul(2))
    assertEquals(10, value.a)
    assertEquals(10, value.y)
  }

  @Ignore
  @Test
  def testUpdate {
    val value = Generic(new A(10))
    // TODO: Fix compilation of this ("macros cannot be partially applied"):
    // value.a = 11
    // assertEquals(11, value.a)
  }

  @Test
  def testNumerics {
    val a = Generic(10)
    assertEquals(12, a + 2)
    assertEquals(8, a - 2)
    assertEquals(20, a * 2)
    assertEquals(5, a / 2)
    assertEquals(10.0, a.toDouble, 0)
  }

  @Test
  def testAlternatives {
    def test[N](a: Generic.Numeric[N]) {
      assertEquals(12, a.toInt)
    }
    test(Generic(12: Byte))
    test(Generic(12: Short))
    test(Generic(12))
    test(Generic(12L))
    test(Generic(12.0))
    test(Generic(12.0f))

    // TODO
  }
}
