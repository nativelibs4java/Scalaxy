package scalaxy.streams

package test

import org.junit._
import org.junit.runner.RunWith
import org.junit.runners.Parameterized.Parameters

import scala.collection.JavaConversions._

import IntegrationTests.{streamMsg, potentialSideEffectMsgs}

class StrategyTest extends StreamComponentsTestBase with StreamTransforms {
  scalaxy.streams.impl.verbose = true
  scalaxy.streams.impl.veryVerbose = false

  val newObjectRx = raw".*java\.lang\.Object\.<init>.*"
  val fnRx = raw".*scala\.Function0\.apply.*"

  @Test
  def testPrints {
    val src = """
      (0 to 1).map(i => { print(i); i }).map(_ * 2).map(i => { print(i); i });
      println()
    """

    { import scalaxy.streams.strategy.safer
      testMessages(src, streamMsg("Range.map.map -> IndexedSeq")) }

    { import scalaxy.streams.strategy.safe
      testMessages(src, streamMsg("Range.map.map -> IndexedSeq")) }

    { import scalaxy.streams.strategy.aggressive
      testMessages(src, streamMsg("Range.map.map.map -> IndexedSeq").
        copy(warnings = potentialSideEffectMsgs("scala.Predef.print"))) }

    { import scalaxy.streams.strategy.foolish
      testMessages(src, streamMsg("Range.map.map.map -> IndexedSeq").
        copy(warnings = potentialSideEffectMsgs("scala.Predef.print"))) }
  }


  @Test
  def testMap2FunctionApply {
    val src = "(0 to 2).map(i => () => i).map(f => (f(), f)).map(_._2())"

    { import scalaxy.streams.strategy.safer
      testMessages(src, streamMsg("Range.map.map -> IndexedSeq")) }

    { import scalaxy.streams.strategy.safe
      testMessages(src, streamMsg("Range.map.map -> IndexedSeq")) }

    { import scalaxy.streams.strategy.aggressive
      testMessages(src, streamMsg("Range.map.map.map -> IndexedSeq"),
        expectWarningRegexp = Some(List(fnRx, fnRx))) }

    { import scalaxy.streams.strategy.foolish
      testMessages(src, streamMsg("Range.map.map.map -> IndexedSeq"),
        expectWarningRegexp = Some(List(fnRx, fnRx))) }
  }


  @Test
  def testInterruptedStream {
    val src = "(0 to 2).map(i => () => i).map(_()).takeWhile(_ < 1)"

    { import scalaxy.streams.strategy.safer
      testMessages(src, streamMsg("Range.map.map -> IndexedSeq")) }

    { import scalaxy.streams.strategy.safe
      testMessages(src, streamMsg("Range.map.map -> IndexedSeq")) }

    { import scalaxy.streams.strategy.aggressive
      testMessages(src, streamMsg("Range.map.map.takeWhile -> IndexedSeq"),
        expectWarningRegexp = Some(List(fnRx))) }

    { import scalaxy.streams.strategy.foolish
      testMessages(src, streamMsg("Range.map.map.takeWhile -> IndexedSeq"),
        expectWarningRegexp = Some(List(fnRx))) }
  }

  @Test
  def testMapFunctionApply {
    val src = "(0 to 2).map(i => (() => i)())"

    { import scalaxy.streams.strategy.safer
      testMessages(src, streamMsg("Range.map -> IndexedSeq")) }

    { import scalaxy.streams.strategy.safe
      testMessages(src, streamMsg("Range.map -> IndexedSeq")) }

    { import scalaxy.streams.strategy.aggressive
      testMessages(src, streamMsg("Range.map -> IndexedSeq"),
        expectWarningRegexp = Some(List(fnRx))) }
  }

  // @Ignore
  @Test
  def testOutsider {
    val src = """
      def outsider[A](a: A) = a
      print((0 to 1).map(outsider).map(_.toString + new Object().toString).map(outsider))
    """

    val outsiderRx = raw".*outsider.*"

    { import scalaxy.streams.strategy.safer
      testMessages(src, streamMsg("Range.map -> IndexedSeq")) }

    { import scalaxy.streams.strategy.safe
      testMessages(src, streamMsg("Range.map -> IndexedSeq")) }

    { import scalaxy.streams.strategy.aggressive
      testMessages(src, streamMsg("Range.map.map.map -> IndexedSeq"),
        expectWarningRegexp = Some(List(outsiderRx, newObjectRx, outsiderRx))) }

    { import scalaxy.streams.strategy.foolish
      testMessages(src, streamMsg("Range.map.map.map -> IndexedSeq"),
        expectWarningRegexp = Some(List(outsiderRx, newObjectRx, outsiderRx))) }
  }

  // @Ignore
  @Test
  def testTwoSideEffects {
    val src = """
      var tot = 0;
      for (i <- 0 until 2; x = new AnyRef) { tot += i }
    """

    val totRx = raw".*\.tot\b.*"

    { import scalaxy.streams.strategy.safer
      testMessages(src, streamMsg("Range.map -> IndexedSeq")) }

    { import scalaxy.streams.strategy.safe
      testMessages(src, streamMsg("Range.map -> IndexedSeq")) }

    { import scalaxy.streams.strategy.aggressive
      testMessages(src, streamMsg("Range.map.foreach"),
        expectWarningRegexp = Some(List(newObjectRx, totRx))) }

    { import scalaxy.streams.strategy.foolish
      testMessages(src, streamMsg("Range.map.foreach"),
        expectWarningRegexp = Some(List(newObjectRx, totRx))) }
  }

  // @Ignore
  @Test
  def testProbablySafe {
    val src = """
      import language.reflectiveCalls
      type Tpe = { def ++(rhs: Any): Any }
      def f(v: Array[Tpe]) = v.map(_ ++ "ha!").filter(_.toString == null).map(_.hashCode)
    """

    { import scalaxy.streams.strategy.safer
      testMessages(src, streamMsg(
        "Array.map -> ArrayOps"//, "Array.filter -> Array", "Array.map -> Array"
      )) }

    { import scalaxy.streams.strategy.safe
      testMessages(src, streamMsg("Array.map.filter.map -> Array")) }
  }

  @Test
  def testFoolishListMap {
    val src = "val c = List(1, 2); c.map(_ * 2)"

    { import scalaxy.streams.strategy.safer
      testMessages(src, CompilerMessages()) }

    { import scalaxy.streams.strategy.foolish
      testMessages(src, streamMsg("List.map -> List")) }
  }

  @Test
  def testFoolishListFilter {
    val src = "val c = List(1, 2); c.filter(_ < 2)"

    { import scalaxy.streams.strategy.safer
      testMessages(src, CompilerMessages()) }

    { import scalaxy.streams.strategy.foolish
      testMessages(src, streamMsg("List.filter -> List")) }
  }

  @Test
  def testFoolishArrayTakeWhile {
    val src = "val c = Array(1, 2); c.takeWhile(_ < 2)"

    { import scalaxy.streams.strategy.safer
      testMessages(src, CompilerMessages()) }

    { import scalaxy.streams.strategy.foolish
      testMessages(src, streamMsg("Array.takeWhile -> Array")) }
  }

  @Test
  def testFoolishArrayDropWhile {
    val src = "val c = Array(1, 2); c.dropWhile(_ < 2)"

    { import scalaxy.streams.strategy.safer
      testMessages(src, CompilerMessages()) }

    { import scalaxy.streams.strategy.foolish
      testMessages(src, streamMsg("Array.dropWhile -> Array")) }
  }
}
