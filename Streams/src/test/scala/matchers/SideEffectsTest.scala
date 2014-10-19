package scalaxy.streams
package test

import org.junit._
import org.junit.Assert._

object SideEffectsTest {
  def foo: Int = ???
  var list = List[Int]()
}

class SideEffectsTest extends StreamComponentsTestBase with SideEffects {
  import global._
  import SideEffectSeverity._

  def sideEffects(tree: Tree): List[SideEffectSeverity] = {
    val effects = analyzeSideEffects(typecheck(tree))
    // effects.foreach(println(_))
    effects.map(_.severity)
  }

  @Test
  def safeCases {
    assertEquals(List(), sideEffects(
      q"(x: Int) => x + 1"))
    assertEquals(List(), sideEffects(
      q"""(x: String) => x + "" """))
    assertEquals(List(), sideEffects(
      q"(x: List[Int]) => x ++ List(1, 2)"))
    assertEquals(List(), sideEffects(
      q"(x: List[Int]) => 1 :: x"))
    assertEquals(List(), sideEffects(
      q"(x: Set[Int]) => x + 1"))
    assertEquals(List(), sideEffects(
      q"(x: Set[Int]) => x ++ Set(1)"))
    assertEquals(List(), sideEffects(
      q"{ val x = 10; x + 1 }"))
    assertEquals(List(), sideEffects(
      q"{ var x = 10; x += 1 }"))
    assertEquals(List(), sideEffects(
      q"{ def x = 10; x }"))
  }

  @Test
  def probablySafeCases {
    assertEquals(List(ProbablySafe), sideEffects(
      q"(x: { def +(a: Any): Any }) => x + 1"))
    assertEquals(List(ProbablySafe), sideEffects(
      q"(x: { def -(a: Any): Any }) => x - 1"))
    assertEquals(List(ProbablySafe), sideEffects(
      q"(x: { def *(a: Any): Any }) => x * 1"))
    assertEquals(List(ProbablySafe), sideEffects(
      q"(x: { def -(a: Any): Any }) => x - 1"))
    assertEquals(List(ProbablySafe), sideEffects(
      q"(x: { def ++(a: Any): Any }) => x ++ 1"))
    assertEquals(List(ProbablySafe), sideEffects(
      q"(x: { def --(a: Any): Any }) => x -- 1"))
    assertEquals(List(ProbablySafe), sideEffects(
      q"(x: Any) => x.toString"))
    assertEquals(List(ProbablySafe), sideEffects(
      q"(x: Any) => x.hashCode"))

    assertEquals(List(ProbablySafe), sideEffects(
      q"(x: collection.mutable.Set[Int]) => x + 1"))
    assertEquals(List(ProbablySafe), sideEffects(
      q"(x: collection.mutable.Set[Int]) => x ++ Set(1)"))
  }

  @Test
  def unsafeCases {
    assertEquals(List(Unsafe), sideEffects(
      q"(x: { def beh: Any }) => x.beh"))
    assertEquals(List(Unsafe), sideEffects(
      q"(x: { def +=(a: Any): Any }) => x += 1"))
    assertEquals(List(Unsafe), sideEffects(
      q"(x: { def -=(a: Any): Any }) => x -= 1"))

    assertEquals(List(Unsafe), sideEffects(
      q"(x: Int) => x + scalaxy.streams.test.SideEffectsTest.foo"))
    assertEquals(List(Unsafe), sideEffects(
      q"(x: List[Int]) => scalaxy.streams.test.SideEffectsTest.list ++= x"))
    assertEquals(List(Unsafe), sideEffects(
      q"(x: collection.mutable.ListBuffer[Int]) => x += 1"))
    assertEquals(List(Unsafe), sideEffects(
      q"System.getProperty(null)"))
  }

  @Test
  def unsafeCasesThatShouldBeRelaxedToProbablySafe {
    assertEquals(List(Unsafe), sideEffects(q"""{
      var x: collection.mutable.ListBuffer[Int] = null
      x += 1
    }"""))
  }
}
