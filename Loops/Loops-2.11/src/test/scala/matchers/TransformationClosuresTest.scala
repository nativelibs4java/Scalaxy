package scalaxy.loops
package test

import org.junit._
import Assert._

class TransformationClosureTest extends StreamComponentsTestBase with TransformationClosures {
  import global._

  @Test
  def testNoOpTuple2 {
    val f = typeCheck(q"""
      (p: (Int, Int)) => p match {
        case pp @ (x, y) =>
          (x, y)
      }
    """)
    val SomeTransformationClosure(
      tc @ TransformationClosure(
        TupleValue(
          List(
            ScalarValue(S("x"), EmptyTree),
            ScalarValue(S("y"), EmptyTree)),
          S("pp")),
        Nil,
        TupleValue(
          List(
            ScalarValue(S("x"), EmptyTree),
            ScalarValue(S("y"), EmptyTree)),
          NoSymbol))) = f

    val List() = tc.getPreviousReferencedTupleAliasPaths(Set()).toList
  }

  @Test
  def testTupleAliasRef {
    val f = typeCheck(q"""
      (p: (Int, Int)) => p match {
        case pp @ (x, y) =>
          println(pp)

          
          (x, y)
      }
    """)
    val SomeTransformationClosure(tc) = f

    val List(RootTuploidPath) = tc.getPreviousReferencedTupleAliasPaths(Set()).toList
  }

  @Test
  def testNoOpScalar {
    val f = typeCheck(q"(x: Int) => x")

    val SomeTransformationClosure(tc @ TransformationClosure(inputs, statements, outputs)) = f
    val ScalarValue(S("x"), EmptyTree) = inputs
    val ScalarValue(S("x"), EmptyTree) = outputs
  }

  @Test
  def testScalarToTuple {
    val f = typeCheck(q"(x: Int) => (1, x)")

    val SomeTransformationClosure(tc @ TransformationClosure(inputs, statements, outputs)) = f
    val ScalarValue(S("x"), EmptyTree) = inputs
    val TupleValue(
      List(
        ScalarValue(NoSymbol, Literal(Constant(1))),
        ScalarValue(S("x"), EmptyTree)),
      NoSymbol) = outputs

    val List() = tc.getPreviousReferencedTupleAliasPaths(Set()).toList
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
