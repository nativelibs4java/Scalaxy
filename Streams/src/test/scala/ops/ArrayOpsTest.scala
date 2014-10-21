package scalaxy.streams
package test

import org.junit._
import org.junit.Assert._

class ArrayOpsTest extends StreamComponentsTestBase with StreamTransforms {
  import global._

  @Test
  def testExtraction {
    val SomeArrayOpsOp(_, ArrayOpsOp) = typecheck(q"genericArrayOps(Array[Any]())")
    val SomeArrayOpsOp(_, ArrayOpsOp) = typecheck(q"refArrayOps(Array[AnyRef]())")
    val SomeArrayOpsOp(_, ArrayOpsOp) = typecheck(q"intArrayOps(Array[Int]())")
    val SomeArrayOpsOp(_, ArrayOpsOp) = typecheck(q"longArrayOps(Array[Long]())")
    val SomeArrayOpsOp(_, ArrayOpsOp) = typecheck(q"byteArrayOps(Array[Byte]())")
    val SomeArrayOpsOp(_, ArrayOpsOp) = typecheck(q"shortArrayOps(Array[Short]())")
    val SomeArrayOpsOp(_, ArrayOpsOp) = typecheck(q"charArrayOps(Array[Char]())")
    val SomeArrayOpsOp(_, ArrayOpsOp) = typecheck(q"booleanArrayOps(Array[Boolean]())")
    val SomeArrayOpsOp(_, ArrayOpsOp) = typecheck(q"floatArrayOps(Array[Float]())")
    val SomeArrayOpsOp(_, ArrayOpsOp) = typecheck(q"doubleArrayOps(Array[Double]())")
  }
}
