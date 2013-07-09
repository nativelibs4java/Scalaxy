package scalaxy.debug.test

import org.junit._
import org.junit.Assert._

import scalaxy.reified.{ reify, ReifiedValue, ReifiedFunction }

class ReifiedTest {
  
  @Test
  def testValue = {
    val x = 10
    val r = reify(10 * x)
    assertTrue(r.isInstanceOf[ReifiedValue[_]])
    assertEquals(Seq(10), r.captures.toSeq)
  }
  
  @Test
  def testFunction = {
    val x = 10
    val r = reify((y: Int) => x * y)
    assertTrue(r.isInstanceOf[ReifiedFunction[_, _]])
    assertEquals(Seq(10), r.captures.toSeq)
  }
}
