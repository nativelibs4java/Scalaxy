package scalaxy.streams

package test

import org.junit._
import org.junit.runner.RunWith
import org.junit.runners.Parameterized.Parameters

import scala.collection.JavaConversions._

import IntegrationTests.{streamMsg, potentialSideEffectMsgs}

/** This is just a testbed for "fast" manual tests */
class AdHocManualTest
    extends StreamComponentsTestBase
    with StreamTransforms
{
  import global._

  scalaxy.streams.impl.verbose = true
  // scalaxy.streams.impl.veryVerbose = true
  // scalaxy.streams.impl.debug = true

  val fnRx = raw".*scala\.Function0\.apply.*"

  // @Ignore
  // @Test
  // def testArrayMapFilterMap {
  //   // (_: Array[Int]).map(_ + 2).filter(_ < 3).map(_.hashCode)
  //   val SomeStream(stream) = typecheck(q"""
  //     (null: Array[String]).map(_ + "ha").filter(_.length < 3).map(_.hashCode)
  //   """)
  //   val Stream(_, ArrayStreamSource(_, _, _), ops, ArrayBuilderSink, false) = stream
  //   val List(ArrayOpsOp, MapOp(_, _), ArrayOpsOp, FilterOp(_, false, "filter"), ArrayOpsOp, MapOp(_, _)) = ops
  // }

  @Ignore
  @Test
  def testFlatMap {
    val src = """
      List(Some(List(1)), None).flatMap(_.getOrElse(List(-1)))
    """

    { import scalaxy.streams.strategy.safe
      testMessages(src, streamMsg("Option.flatMap -> Option")) }
  }

  @Ignore
  @Test
  def testFM {
    val src = """
      for ((a, b) <- List(null, (1, 2)); if a < b) yield a + b
    """

    { import scalaxy.streams.strategy.aggressive
      testMessages(src, streamMsg("List.flatMap -> List")) }
  }
}
