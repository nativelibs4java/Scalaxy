package scalaxy.loops
package test

import org.junit._
import Assert._

class StreamSourcesTest extends StreamComponentsTestBase with StreamOps {
  import global._

  @Test
  def testInlineRangeExtractor {
    val v1 @ SomeInlineRangeStreamSource(InlineRangeStreamSource(_, _, 1, true, _)) = typeCheck(q"1 to 10")
    val SomeStreamSource(_) = v1

    val v2 @ SomeInlineRangeStreamSource(InlineRangeStreamSource(_, _, 1, false, _)) = typeCheck(q"1 until 10")
    val SomeStreamSource(_) = v2

    val v3 @ SomeInlineRangeStreamSource(InlineRangeStreamSource(_, _, -2, true, _)) = typeCheck(q"10 to 1 by -2")
    val SomeStreamSource(_) = v3

    val v4 @ SomeInlineRangeStreamSource(InlineRangeStreamSource(_, _, -2, false, _)) = typeCheck(q"10 until 1 by -2")
    val SomeStreamSource(_) = v4
  }
}
