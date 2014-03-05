package scalaxy.loops

package test

import org.junit._
import Assert._

class StreamsTest extends StreamComponentsTestBase with Streams with StreamOps {
  import global._

  @Test
  def testMapMapFilterMap {
    val SomeStream(Stream(ArrayStreamSource(_), ops, ArrayBufferSink)) = typeCheck(q"""
      //Array(1).map(_ + 1).map(_ * 10).filter(_ < 10)
      (1 to 10).map(_ + 1)
    """)
    println(s"ops = " + ops.mkString("\n\t"))
    val MapOp(_, _, _) :: MapOp(_, _, _) :: FilterOp(_, _) :: Nil = ops
  }

  @Test
  def testFlatMap {
    val SomeStream(Stream(source, ops, sink)) = typeCheck(q"""
      for (a <- Array(Array(1)); len = a.length; v <- a) yield (a, len, v)
    """)
    println(s"ops = " + ops.mkString("\n\t"))
  }

  @Test
  def testMaps {
    val SomeStream(Stream(source, ops, sink)) = typeCheck(q"""
      for ((a, i) <- Array(Array(1)).zipWithIndex; len = a.length; if len < i) {
        println(a + ", " + len + ", " + i)
      }
    """)
    println(s"ops = " + ops.mkString("\n\t"))
  }
}
