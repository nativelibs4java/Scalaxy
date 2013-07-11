package scalaxy.reified.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror
import scala.tools.reflect.ToolBox

import scalaxy.reified.{ reify, ReifiedValue, ReifiedFunction }

class ReificationTest extends TestUtils {
  @Ignore
  @Test 
  def testFlat {
    val x: AnyRef = Seq(1, 2, 3)
    val y = 12 + " things"
    
    val a: AnyRef = reify(Seq(x, "blah"))
    val b = reify((y, a))
    
    assertEquals(Seq(x), a.asInstanceOf[ReifiedValue[_]].reification.capturedValues)
    assertEquals(Seq(y, a), b.reification.capturedValues)
    
    assertEquals(b.value, eval(b.expr().tree))
    //println(b.reification.taggedExpr)
  }
}
