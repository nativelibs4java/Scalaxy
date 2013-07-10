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
  
  @Test
  def testFunctionComposition = {
    def compose(capture1: Int): ReifiedFunction[Int, Int] = {
      val capture2 = 666
      val f = reify((x: Int) => x * x + capture2)
      val capture3 = 1234
      val g = reify((x: Int) => capture1 + capture3)
      
      f.compose(g)
    }
    
    val comp10 = compose(10)
    assertEquals(Seq(666, 10, 1234), comp10.captures.toSeq)
    
    val comp100 = compose(100)
    assertEquals(Seq(666, 100, 1234), comp100.captures.toSeq)
    
    println(comp10.expr.tree)
    println(comp100.expr.tree)
  }
}
