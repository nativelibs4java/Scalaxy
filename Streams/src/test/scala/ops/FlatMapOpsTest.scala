package scalaxy.streams
package test

import org.junit._
import org.junit.Assert._

class FlatMapOpsTest extends StreamComponentsTestBase with StreamTransforms {
  import global._

  @Test
  def testFlatMapExtractor {
    val v @ SomeFlatMapOp(_, FlatMapOp(_, _, _, _)) = typecheck(q"""
      Array(1).flatMap(v => Seq(v + 1))
    """)
    val SomeStreamOp(_, _ :: _ :: Nil) = v
  }
}
