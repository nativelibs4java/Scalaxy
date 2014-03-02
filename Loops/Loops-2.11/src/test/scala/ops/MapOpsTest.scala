package scalaxy.loops
package test

import org.junit._
import Assert._

class MapOpsTest extends StreamComponentsTestBase with StreamOps {
  import global._

  @Test
  def testMapExtractor {
    val v @ SomeMapOp(_, MapOp(_, _)) = typeCheck(q"Array(1).map(_ + 1)")
    val SomeStreamOp(_, _ :: Nil) = v
  }
}
