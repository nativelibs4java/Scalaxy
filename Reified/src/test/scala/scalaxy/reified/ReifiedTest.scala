package scalaxy.reified.test

import org.junit._
import org.junit.Assert._

import scalaxy.reified.{ reify, ReifiedValue, ReifiedFunction }

class ReifiedTest {
  
  @Test
  def testValue = {
    val x = 10
    val r = reify(100 * x)
    assertTrue(r.isInstanceOf[ReifiedValue[_]])
    assertEquals(Seq(10), r.captures.toSeq)
    assertEquals("100.*(10)", r.expr.tree.toString)
  }
  
  @Test
  def testFunction = {
    val x = 10
    val y = 20
    val r = reify((v: Int) => x * v * y)
    assertTrue(r.isInstanceOf[ReifiedFunction[_, _]])
    assertEquals(Seq(10, 20), r.captures.toSeq)
    assertEquals("((v: Int) => 10.*(v).*(20))", r.expr.tree.toString)
  }
}
