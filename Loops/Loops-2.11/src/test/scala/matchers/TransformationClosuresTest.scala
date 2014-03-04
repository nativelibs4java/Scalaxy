package scalaxy.loops
package test

import org.junit._
import Assert._

class TransformationClosureTest extends StreamComponentsTestBase with TransformationClosures {
  import global._

  val EmptyName: TermName = ""

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
          inputValues,
          S("pp"),
          EmptyName),
        Nil,
        TupleValue(
          outputValues,
          NoSymbol,
          EmptyName))) = f

    val List(
      (0, ScalarValue(EmptyTree, S("x"), EmptyName)),
      (1, ScalarValue(EmptyTree, S("y"), EmptyName))) = inputValues.toList

    assertEquals(inputValues, outputValues)

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
    val ScalarValue(EmptyTree, S("x"), EmptyName) = inputs
    val ScalarValue(EmptyTree, S("x"), EmptyName) = outputs
  }

  @Test
  def testScalarToTuple {
    val f = typeCheck(q"(x: Int) => (1, x)")

    val SomeTransformationClosure(tc @ TransformationClosure(inputs, statements, outputs)) = f
    val ScalarValue(EmptyTree, S("x"), EmptyName) = inputs
    val TupleValue(
      values,
      NoSymbol,
      EmptyName) = outputs

    val List(
      (0, ScalarValue(Literal(Constant(1)), NoSymbol, EmptyName)),
      (1, ScalarValue(EmptyTree, S("x"), EmptyName))) = values.toList

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
