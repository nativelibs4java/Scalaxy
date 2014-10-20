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

  // @Ignore
  @Test
  def testOutsider {
    val src = """
      def outsider[A](a: A) = a
      print((0 to 10).map(outsider).map(_.toString + new Object().toString).map(outsider))
    """

    { import scalaxy.streams.strategy.safer
      testMessages(src, streamMsg("Range.map -> IndexedSeq")) }

    // TODO: proper warnings regexp instead of just dummy count
    { import scalaxy.streams.strategy.safe
      testMessages(src, streamMsg("Range.map -> IndexedSeq"),
        expectWarningCount = Some(2)) }

    { import scalaxy.streams.strategy.aggressive
      testMessages(src, streamMsg("Range.map.map.map -> IndexedSeq"),
        expectWarningCount = Some(5)) }
  }

  // @Ignore
  @Test
  def testProbablySafe {
    val src = """
      import language.reflectiveCalls
      type Tpe = { def +++(rhs: Any): Any }
      def f(v: List[Tpe]) = v.map(_ +++ "ha!").filter(_.toString == null).map(_.hashCode)
    """

    { import scalaxy.streams.strategy.safer
      testMessages(src, streamMsg(
        "List.map -> List"//, "List.filter -> List", "List.map -> List"
      )) }

    { import scalaxy.streams.strategy.safe
      testMessages(src, streamMsg("List.map.filter.map -> List")) }
  }
}
