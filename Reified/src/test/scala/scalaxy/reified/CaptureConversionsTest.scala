package scalaxy.reified.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror

import scalaxy.reified.reify

class CaptureConversionsTest extends TestUtils {

  def testValue(v: Any, str: String = null, predicate: Any => Boolean = null) = {
    val r = reify(if (true) v else 0)
    assertEquals(Seq(v), r.capturedValues)
    try {
      val value = r.compile()()
      if (v.getClass.isArray) {
        val meth = classOf[Assert].getMethod("assertArrayEquals", v.getClass, v.getClass)
        meth.invoke(null, v.asInstanceOf[AnyRef], value.asInstanceOf[AnyRef])
      } else {
        assertEquals("Got " + value + ": " + value.getClass.getName, v, value)
      }
      if (predicate != null) {
        assertTrue("Predicate failed for " + value, predicate(value))
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
    testValue(Array(1, 2))
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
