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
          inputValues,
          S("pp")),
        Nil,
        TupleValue(
          outputValues,
          None))) = f

    val List(
      (0, ScalarValue(EmptyTree, S("x"))),
      (1, ScalarValue(EmptyTree, S("y")))) = inputValues.toList

    assertEquals(inputValues, outputValues)

    val List() = tc.getPreviousReferencedPaths(Set()).toList
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

    val List(RootTuploidPath) = tc.getPreviousReferencedPaths(Set()).toList
  }

  @Test
  def testNoOpScalar {
    val f = typeCheck(q"(x: Int) => x")

    val SomeTransformationClosure(tc @ TransformationClosure(inputs, statements, outputs)) = f
    val ScalarValue(EmptyTree, S("x")) = inputs
    val ScalarValue(EmptyTree, S("x")) = outputs
  }

  @Test
  def testScalarToPrintln {
    val f = typeCheck(q"(x: Int) => println(x)")

    val SomeTransformationClosure(tc @ TransformationClosure(inputs, statements, outputs)) = f
    val ScalarValue(EmptyTree, S("x")) = inputs
    val ScalarValue(q"scala.this.Predef.println(x)", None) = outputs
  }

  @Test
  def testScalarToTuple {
    val f = typeCheck(q"(x: Int) => (1, x)")

    val SomeTransformationClosure(tc @ TransformationClosure(inputs, statements, outputs)) = f
    val ScalarValue(EmptyTree, S("x")) = inputs
    val TupleValue(
      values,
      None) = outputs

    val List(
      (0, ScalarValue(Literal(Constant(1)), None)),
      (1, ScalarValue(EmptyTree, S("x")))) = values.toList

    val List() = tc.getPreviousReferencedPaths(Set()).toList
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
