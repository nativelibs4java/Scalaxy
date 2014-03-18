package scalaxy.streams
package test

import org.junit._
import org.junit.Assert._

class CoerceOpsTest extends StreamComponentsTestBase with StreamTransforms with CoerceOps {
  import global._

  @Test
  def testCoerceExtractor {
    val v @ SomeCoerceOp(_, CoerceOp(_, _)) = typeCheck(q"""Array(Array(1)).zipWithIndex.withFilter(
      (item2: (Array[Int], Int)) => (item2: (Array[Int], Int) @unchecked) match {
        case ((a @ _), (i @ _)) => true
        case _ => false
      }
    )""")
    // val SomeStreamOp(_, _ :: Nil) = v
  }
}
