package scalaxy.streams
package test

import org.junit._
import org.junit.Assert._

class ZipWithIndexOpsTest extends StreamComponentsTestBase with StreamTransforms {
  import global._

  @Test
  def testMapExtractor {
    val v @ SomeZipWithIndexOp(_, ZipWithIndexOp(_)) = typecheck(q"Array(1).zipWithIndex")
    val SomeStreamOp(_, _ :: _ :: Nil) = v
  }
}
