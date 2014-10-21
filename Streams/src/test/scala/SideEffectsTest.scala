package scalaxy.streams
package test

import org.junit._
import org.junit.Assert._

object SideEffectsTest {
  def foo: Int = ???
  var list = List[Int]()
}

class SideEffectsTest
    extends StreamComponentsTestBase
    with SideEffectsDetection
    with StreamTransforms
{
  import global._
  import SideEffectSeverity._

  def expectSideEffectSeverities(expected: List[SideEffectSeverity], tree: Tree) {
    val effects = analyzeSideEffects(typecheck(tree))
    val actual = effects.map(_.severity)
    if (actual != expected) {
      effects.foreach(println(_))
      assertEquals(tree.toString,
        expected, actual)
    }
  }

  @Test
  def safeCases {
    expectSideEffectSeverities(List(),
      q""" "x".hashCode """)
    expectSideEffectSeverities(List(),
      q"(x: Int) => x + 1")
    expectSideEffectSeverities(List(),
      q"""(x: String) => x + "" """)
    expectSideEffectSeverities(List(),
      q"(x: List[Int]) => x ++ List(1, 2)")
    expectSideEffectSeverities(List(),
      q"(x: List[Int]) => 1 :: x")
    expectSideEffectSeverities(List(),
      q"{ val x = 10; x + 1 }")
    expectSideEffectSeverities(List(),
      q"{ var x = 10; x += 1 }")
    expectSideEffectSeverities(List(),
      q"{ def x = 10; x }")

    expectSideEffectSeverities(List(),
      q"Array.canBuildFrom[String]")
  }

  @Test
  def probablySafeCases {
    expectSideEffectSeverities(List(ProbablySafe),
      q"(x: { def +(a: Any): Any }) => x + 1")
    expectSideEffectSeverities(List(ProbablySafe),
      q"(x: { def -(a: Any): Any }) => x - 1")
    expectSideEffectSeverities(List(ProbablySafe),
      q"(x: { def *(a: Any): Any }) => x * 1")
    expectSideEffectSeverities(List(ProbablySafe),
      q"(x: { def -(a: Any): Any }) => x - 1")
    expectSideEffectSeverities(List(ProbablySafe),
      q"(x: { def ++(a: Any): Any }) => x ++ 1")
    expectSideEffectSeverities(List(ProbablySafe),
      q"(x: { def --(a: Any): Any }) => x -- 1")
    expectSideEffectSeverities(List(ProbablySafe),
      q"(x: Any) => x.toString")
    expectSideEffectSeverities(List(ProbablySafe),
      q"(x: Any) => x.hashCode")

    expectSideEffectSeverities(List(ProbablySafe),
      q"(x: collection.mutable.Set[Int]) => x + 1")
    expectSideEffectSeverities(List(ProbablySafe),
      q"(x: collection.mutable.Set[Int]) => x ++ Set(1)")
  }

  @Test
  def unsafeCases {
    expectSideEffectSeverities(List(Unsafe),
      q"new Object()")
    expectSideEffectSeverities(List(Unsafe),
      q"new Object().toString")
    expectSideEffectSeverities(List(Unsafe),
      q"(x: { def beh: Any }) => x.beh")
    expectSideEffectSeverities(List(Unsafe),
      q"(x: { def +=(a: Any): Any }) => x += 1")
    expectSideEffectSeverities(List(Unsafe),
      q"(x: { def -=(a: Any): Any }) => x -= 1")

    expectSideEffectSeverities(List(Unsafe),
      q"(x: Int) => x + scalaxy.streams.test.SideEffectsTest.foo")
    expectSideEffectSeverities(List(Unsafe),
      q"(x: List[Int]) => scalaxy.streams.test.SideEffectsTest.list ++= x")
    expectSideEffectSeverities(List(Unsafe),
      q"(x: collection.mutable.ListBuffer[Int]) => x += 1")
    expectSideEffectSeverities(List(Unsafe),
      q"System.getProperty(null)")
  }

  @Test
  def unsafeCasesThatShouldBeRelaxedToProbablySafe {
    expectSideEffectSeverities(List(Unsafe),
      q"""{
        var x: collection.mutable.ListBuffer[Int] = null
        x += 1
      }""")
  }

  @Test
  def probablySafeCasesThatShouldBeRelaxedToSafe {
    expectSideEffectSeverities(List(ProbablySafe),
      q"(x: Set[Int]) => x + 1")
    expectSideEffectSeverities(List(ProbablySafe),
      q"(x: Set[Int]) => x ++ Set(1)")
  }

  @Test
  def collections {
    // Note: these will likely be rewritten away by Scalaxy's optimizer.
    expectSideEffectSeverities(List(),
      q"""{
        val n = 10;
        for (i <- 0 to n;
             j <- i to 1 by -1;
             if i % 2 == 1)
          yield { i + j }
      }""")
    expectSideEffectSeverities(List(),
      q"""
        for ((a, i) <- Array(Array(1)).zipWithIndex; len = a.length; if len < i) yield {
          a + ", " + len + ", " + i
        }
      """)
  }
}
