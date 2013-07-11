package scalaxy.reified.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror
import scala.tools.reflect.ToolBox

import scalaxy.reified.reify

class CaptureConversionsTest extends TestUtils {
  
  def testValue(v: Any, str: String = null, predicate: Any => Boolean = null) = {
    val r = reify(if (true) v else 0)
    //println(s"Type of $v once captured is ${r.reification.captures.map(_._2).head}")
    assertEquals(Seq(v), r.reification.capturedValues)
    try {
      val tree = r.expr().tree
      val eval = toolbox.eval(toolbox.resetAllAttrs(tree))
      assertEquals(v, eval)
      if (predicate != null) {
        assertTrue("Predicate failed for " + eval, predicate(eval))
      }
    } catch {
      case ex: Throwable =>
        println("Error when evaluating " + r)
        ex.printStackTrace(System.out)
        throw ex
    }
    //assertEquals(Option(str).getOrElse(v.toString), r.expr().tree.toString)
  }
  
  @Test
  def testConstants {
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
  }
  
  @Test
  def testArrays {
    // TODO
    //testValue(Array(1, 2))
  }
  
  @Test
  def testImmutableCollections {
    import scala.collection.immutable._
    
    testValue(Set(1, 2))
    testValue(Seq(1, 2))
    testValue(List(1, 2), predicate = _.isInstanceOf[List[_]])
    //testValue(TreeSet(1, 2), predicate = _.isInstanceOf[TreeSet[_]])
    //testValue(SortedSet(1, 2), predicate = _.isInstanceOf[SortedSet[_]])
    testValue(HashSet(1, 2), predicate = _.isInstanceOf[HashSet[_]])
    testValue(Stack(1, 2), predicate = _.isInstanceOf[Stack[_]])
    testValue(Queue(1, 2), predicate = _.isInstanceOf[Queue[_]])
    testValue(Vector(1, 2), predicate = _.isInstanceOf[Vector[_]])
    testValue(1 to 10, predicate = _.isInstanceOf[Range])
    testValue(1 to 10 by 2, predicate = _.isInstanceOf[Range])
    testValue(10 to 1 by -2, predicate = _.isInstanceOf[Range])
    testValue(1 until 10, predicate = _.isInstanceOf[Range])
    testValue(1 until 10 by 2, predicate = _.isInstanceOf[Range])
    testValue(10 until 1 by -2, predicate = _.isInstanceOf[Range])
    
    //testValue(Map('a' -> 1, 'b' -> 2))
  }
}
