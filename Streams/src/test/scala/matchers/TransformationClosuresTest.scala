package scalaxy.streams
package test

import org.junit._
import org.junit.Assert._

class TransformationClosureTest extends StreamComponentsTestBase with TransformationClosures {
  import global._

  @Test
  def testNoOpTuple2 {
    val f = typecheck(q"""
      (p: (Int, Int)) => p match {
        case pp @ (x, y) =>
          (x, y)
      }
    """)
    val SomeTransformationClosure(
      tc @ TransformationClosure(
        TupleValue(
          _,
          inputValues,
          Some(S("pp")),
          true),
        Nil,
        TupleValue(
          _,
          outputValues,
          None,
          false),
        closureSymbol)) = f

    val List(
      (0, ScalarValue(_, None, Some(S("x")))),
      (1, ScalarValue(_, None, Some(S("y"))))) = inputValues.toList

    assertEquals(inputValues, outputValues)

    val List() = tc.getPreviousReferencedPaths(Set()).toList
  }

  @Test
  def testTupleAliasRef {
    val f = typecheck(q"""
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
    val f = typecheck(q"(x: Int) => x")

    val SomeTransformationClosure(
      tc @ TransformationClosure(inputs, statements, outputs, closureSymbol)) = f
    val ScalarValue(_, None, Some(S("x"))) = inputs
    val ScalarValue(_, None, Some(S("x"))) = outputs
  }

  @Test
  def testScalarToPrintln {
    val f = typecheck(q"(x: Int) => println(x)")

    val SomeTransformationClosure(
      tc @ TransformationClosure(inputs, statements, outputs, closureSymbol)) = f
    val ScalarValue(_, None, Some(S("x"))) = inputs
    val ScalarValue(_, Some(q"scala.this.Predef.println(x)"), None) = outputs
  }

  @Test
  def testScalarToTuple {
    val f = typecheck(q"(x: Int) => (1, x)")

    val SomeTransformationClosure(
      tc @ TransformationClosure(inputs, statements, outputs, closureSymbol)) = f
    val ScalarValue(_, None, Some(S("x"))) = inputs
    val TupleValue(
      _,
      values,
      None,
      false) = outputs

    val List(
      (0, ScalarValue(_, Some(Literal(Constant(1))), None)),
      (1, ScalarValue(_, None, Some(S("x"))))) = values.toList

    val List() = tc.getPreviousReferencedPaths(Set()).toList
  }


  @Test
  def tupleMappedToTuple {
    val f = typecheck(q"""
      (x2: (Int, Int)) => (x2: (Int, Int) @unchecked) match {
        case (x1 @ ((v @ _), (i @ _))) => {
          val c: Int = i.+(2);
          scala.Tuple2.apply[(Int, Int), Int]((v, i), c)
        }
      }
    """)
    val SomeTransformationClosure(tc) = f
    // println(tc)
  }

  @Test
  def scalarMappedToTuple {
    val f = typecheck(q"""
      (array: Array[Int]) => {
        val length: Int = array.length.*(30);
        scala.Tuple2.apply[Array[Int], Int](array, length)
      }
    """)

    val SomeTransformationClosure(tc) = f
    // println(tc)
  }

  @Test
  def simple3Tuple {
    val f = typecheck(q"""
      ((t: ((Int, Int), Int)) => (t: ((Int, Int), Int) @unchecked) match {
        case (((x @ (_: Int)), (y @ (_: Int))), (i @ (_: Int))) =>
          (x + y) % 2 == 0
      })
    """)
    val SomeTransformationClosure(tc) = f
    val TransformationClosure(inputs, statements, outputs, closureSymbol) = tc

    val IntTpe = typeOf[Int]
    val IntIntTpe = typeOf[(Int, Int)]
    val IntIntIntTpe = typeOf[((Int, Int), Int)]

    val TupleValue(IntIntIntTpe, inputValues, None, true) = inputs
    val Seq(0, 1) = inputValues.keys.toSeq.sorted

    val TupleValue(IntIntTpe, subInputValues, None, true) = inputValues(0)
    val Seq(0, 1) = subInputValues.keys.toSeq.sorted
    val ScalarValue(IntTpe, _, Some(S("i"))) = inputValues(1)

    val ScalarValue(IntTpe, _, Some(S("x"))) = subInputValues(0)
    val ScalarValue(IntTpe, _, Some(S("y"))) = subInputValues(1)
  }
  /**

  val f = toolbox.typecheck(q"""
        (x2: (Int, Int)) => (x2: (Int, Int) @unchecked) match {
          case (x1 @ ((v @ _), (i @ _))) => {
            val c: Int = i.+(2);
            scala.Tuple2.apply[(Int, Int), Int]((v, i), c)
          }
        }
      """)
      val f = toolbox.typecheck(q"""
        (array: Array[Int]) => {
          val length: Int = array.length.*(30);
          scala.Tuple2.apply[Array[Int], Int](array, length)
        }
      """)

*/
}
