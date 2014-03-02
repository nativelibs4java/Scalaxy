package scalaxy.loops
package test

import org.junit._
import Assert._

class TransformationClosureTest extends StreamComponentsTestBase with TransformationClosures {
  import global._

  @Test
  def test {
    val f = typeCheck(q"""
      (p: (Int, Int)) => p match {
        case pp @ (x, y) =>
          (x, y)
      }
    """)
    val SomeTransformationClosure(tc) = f
    println(tc)
  }
  /**

  val f = toolbox.typeCheck(q"""
        (x2: (Int, Int)) => (x2: (Int, Int) @unchecked) match {
          case (x1 @ ((v @ _), (i @ _))) => {
            val c: Int = i.+(2);
            scala.Tuple2.apply[(Int, Int), Int]((v, i), c)
          }
        }
      """)
      val f = toolbox.typeCheck(q"""
        (array: Array[Int]) => {
          val length: Int = array.length.*(30);
          scala.Tuple2.apply[Array[Int], Int](array, length)
        }
      """)

*/
}
