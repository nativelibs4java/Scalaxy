package scalaxy.loops

package test

import org.junit._
import Assert._

class StreamsTest extends StreamComponentsTestBase with Streams with StreamOps {
  import global._

  @Test
  def test {
    val SomeStream(Stream(source, ops, sink)) = typeCheck(q"""
      for (a <- Array(Array(1)); len = a.length; v <- a) yield (a, len, v)
    """)
  }
}
