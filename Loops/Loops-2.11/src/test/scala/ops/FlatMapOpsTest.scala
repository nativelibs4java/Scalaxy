package scalaxy.loops
package test

import org.junit._
import org.junit.Assert._

class FlatMapOpsTest extends StreamComponentsTestBase with Streams {
  import global._

  @Test
  def testFlatMapExtractor {
    val v @ SomeFlatMapOp(_, GenericFlatMapOp(_, _, _, _)) = typeCheck(q"""
      Array(1).flatMap(v => Seq(v + 1))
    """)
    val SomeStreamOp(_, _ :: Nil) = v
  }
}
