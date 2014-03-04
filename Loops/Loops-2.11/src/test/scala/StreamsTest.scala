package scalaxy.loops

package test

import org.junit._
import Assert._

class StreamsTest extends StreamComponentsTestBase with Streams with StreamOps {
  import global._

  @Test
  def testFlatMap {
    val SomeStream(Stream(source, ops, sink)) = typeCheck(q"""
      for (a <- Array(Array(1)); len = a.length; v <- a) yield (a, len, v)
    """)
  }

  @Test
  def testMaps {
    val SomeStream(Stream(source, ops, sink)) = typeCheck(q"""
      for ((a, i) <- Array(Array(1)).zipWithIndex; len = a.length; if len < i) {
        println(a + ", " + len + ", " + i)
      }
    """)
  }
}
