package scalaxy.reified.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror

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

}
