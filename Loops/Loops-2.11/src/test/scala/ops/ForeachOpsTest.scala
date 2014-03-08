package scalaxy.loops
package test

import org.junit._
import org.junit.Assert._

class ForeachOpsTest extends StreamComponentsTestBase with Streams {
  import global._

  @Test
  def testForeachExtractor {
    val v @ SomeForeachOp(_, ForeachOp(_, _)) = typeCheck(q"(1 to 10).foreach(println _)")
    val SomeStreamOp(_, _ :: Nil) = v
  }
}
