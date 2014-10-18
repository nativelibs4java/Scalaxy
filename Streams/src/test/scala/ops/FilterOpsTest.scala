package scalaxy.streams
package test

import org.junit._
import org.junit.Assert._

class FilterOpsTest extends StreamComponentsTestBase with StreamTransforms {
  import global._

  @Test
  def testFilterExtractor {
    val v1 @ SomeFilterOp(_, FilterOp(_, false, "filter")) = typecheck(q"Array(1).filter(_ == 0)")
    val SomeStreamOp(_, _ :: _ :: Nil) = v1

    val v2 @ SomeFilterOp(_, FilterOp(_, true, "filterNot")) = typecheck(q"Array(1).filterNot(_ == 0)")
    val SomeStreamOp(_, _ :: _ :: Nil) = v2

    val v3 @ SomeFilterOp(_, FilterOp(_, false, "withFilter")) = typecheck(q"Array(1).withFilter(_ == 0)")
    val SomeStreamOp(_, _ :: _ :: Nil) = v3
  }
}
