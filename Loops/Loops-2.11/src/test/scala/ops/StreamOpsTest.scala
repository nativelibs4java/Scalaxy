package scalaxy.loops
package test

import org.junit._
import Assert._

class StreamOpsTest extends StreamComponentsTestBase with StreamOps {
  import global._

  @Test
  def testFilterExtractor {
    val v1 @ SomeFilterOp(_, FilterOp(_, _)) = typeCheck(q"Array(1).filter(_ == 0)")
    val SomeStreamOp(_, _ :: Nil) = v1

    val v2 @ SomeFilterOp(_, FilterOp(_, _)) = typeCheck(q"Array(1).withFilter(_ == 0)")
    val SomeStreamOp(_, _ :: Nil) = v2
  }

  @Test
  def testMapExtractor {
    val v @ SomeMapOp(_, MapOp(_, _)) = typeCheck(q"Array(1).map(_ + 1)")
    val SomeStreamOp(_, _ :: Nil) = v
  }

  @Test
  def testForeachExtractor {
    val v @ SomeForeachOp(_, ForeachOp(_, _)) = typeCheck(q"(1 to 10).foreach(println _)")
    val SomeStreamOp(_, _ :: Nil) = v
  }
}
