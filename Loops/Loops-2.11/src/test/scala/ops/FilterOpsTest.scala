package scalaxy.loops
package test

import org.junit._
import org.junit.Assert._

class FilterOpsTest extends StreamComponentsTestBase with StreamOps {
  import global._

  @Test
  def testFilterExtractor {
    val v1 @ SomeFilterOp(_, FilterOp(_, _)) = typeCheck(q"Array(1).filter(_ == 0)")
    val SomeStreamOp(_, _ :: Nil) = v1

    val v2 @ SomeFilterOp(_, FilterOp(_, _)) = typeCheck(q"Array(1).withFilter(_ == 0)")
    val SomeStreamOp(_, _ :: Nil) = v2
  }
}
