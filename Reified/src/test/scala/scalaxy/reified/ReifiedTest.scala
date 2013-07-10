package scalaxy.reified.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror
import scala.tools.reflect.ToolBox

import scalaxy.reified.{ reify, ReifiedValue, ReifiedFunction }

class ReifiedTest {
  
  @Test
  def testValue = {
    val x = 10
    val r = reify(100 * x)
    assertTrue(r.isInstanceOf[ReifiedValue[_]])
    assertEquals(Seq(10), r.captures.map(_._1))
    assertEquals("100.*(10)", r.expr().tree.toString)
  }
  
  @Test
  def testFunction = {
    val x = 10
    val y = 20
    val r = reify((v: Int) => x * v * y)
    assertTrue(r.isInstanceOf[ReifiedFunction[_, _]])
    assertEquals(Seq(10, 20), r.captures.map(_._1))
    assertEquals("((v: Int) => 10.*(v).*(20))", r.expr().tree.toString)
  }
  
  @Test
  def testCaptureConversion = {
    val toolbox = currentMirror.mkToolBox()
    
    def testValue(v: Any, str: String = null) = {
      val r = reify(v)
      //println(s"Type of $v once captured is ${r.captures.map(_._2).head}")
      assertEquals(Seq(v), r.captures.map(_._1))
      try {
        val tree = r.expr().tree
        
        assertEquals(v, toolbox.eval(tree))//toolbox.resetAllAttrs(tree)))
      } catch {
        case ex: Throwable =>
          ex.printStackTrace(System.out)
          throw ex
      }
      //assertEquals(Option(str).getOrElse(v.toString), r.expr().tree.toString)
    }
    testValue(true)
    testValue(false)
    testValue(10: Byte)
    testValue(10: Short)
    testValue('1', "'1'")
    testValue(10)
    testValue(10L, "10L")
    testValue(10f)
    testValue(10.0)
    testValue("10", "\"10\"")
    
    import scala.collection.immutable.{ List, Seq, Set, Stack, Vector }
    testValue(List(1, 2))
    testValue(Set(1, 2))
    testValue(Seq(1, 2))
    testValue(Stack(1, 2))
    testValue(Vector(1, 2))
    
    //testValue(Array(1, 2))
    //testValue(Map('a' -> 1, 'b' -> 2))
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
    assertEquals(Seq(10, 1234, 666), comp10.captures.map(_._1))
    
    val comp100 = compose(100)
    assertEquals(Seq(100, 1234, 666), comp100.captures.map(_._1))
    
    //println(comp10.expr().tree)
    //println(comp100.expr().tree)
  }
}
