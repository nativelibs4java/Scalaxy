package scalaxy.streams
package test

import org.junit._
import org.junit.Assert._

class MapOpsTest extends StreamComponentsTestBase with StreamTransforms {
  import global._

  @Test
  def testMapExtractor {
    val v @ SomeMapOp(_, MapOp(_, _, _)) = typeCheck(q"Array(1).map(_ + 1)")
    val SomeStreamOp(_, _ :: Nil) = v
  }
}
