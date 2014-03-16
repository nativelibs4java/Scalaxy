package scalaxy.streams
package test

import org.junit._
import org.junit.Assert._

class FilterOpsTest extends StreamComponentsTestBase with StreamTransforms {
  import global._

  @Test
  def testFilterExtractor {
    val v1 @ SomeFilterOp(_, FilterOp(_, _, false, "filter")) = typeCheck(q"Array(1).filter(_ == 0)")
    val SomeStreamOp(_, _ :: Nil) = v1

    val v2 @ SomeFilterOp(_, FilterOp(_, _, true, "filterNot")) = typeCheck(q"Array(1).filterNot(_ == 0)")
    val SomeStreamOp(_, _ :: Nil) = v2

    val v3 @ SomeFilterOp(_, FilterOp(_, _, false, "withFilter")) = typeCheck(q"Array(1).withFilter(_ == 0)")
    val SomeStreamOp(_, _ :: Nil) = v3
  }
}
