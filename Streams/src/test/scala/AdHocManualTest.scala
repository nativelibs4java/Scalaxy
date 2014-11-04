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
    with PerformanceTestBase
    with StreamTransforms
{
  import global._

  scalaxy.streams.impl.verbose = true
  scalaxy.streams.impl.veryVerbose = true
  scalaxy.streams.impl.debug = true
  scalaxy.streams.impl.quietWarnings = true

  val fnRx = raw".*scala\.Function0\.apply.*"

  // @Ignore
  // @Test
  def testComp {
    // val src = """(
    //   Some[Any](1).orNull
    // )"""
    val src = """
      List(Some(List(1)), None).flatMap(_.getOrElse(List(-1)))
    """
    // val src = """
    //   (
    //     (None: Option[Int]).orElse(None),
    //       Option[Any](null).orElse(None),
    //       Some(1).orElse(None),
    //     (None: Option[Int]).orElse(Some(2)),
    //       Option[Any](null).orElse(Some(2)),
    //       Some(1).orElse(Some(2)),
    //     (None: Option[Int]).orElse(Option(3)),
    //       Option[Any](null).orElse(Option(3)),
    //       Some(1).orElse(Option(3)),
    //     (None: Option[String]).orElse(Option(null)),
    //       Option[String](null).orElse(Option(null)),
    //       Some("a").orElse(Option(null))
    //   )
    // """

    {
      import scalaxy.streams.strategy.foolish
      testMessages(src, streamMsg("Array.map -> Array"))

      // val (_, messages) = compileFast(src)
      // println(messages)
    }
  }

  // @Test
  def testPerf {
    // streamMsg("Range.map.flatMap(Range.map.withFilter.flatMap(Range.map)) -> IndexedSeq"),

    ensureFasterCodeWithSameResult(
      decls = "",
      // val n = 20;
      code = """
        for (i <- 0 to n;
             ii = i * i;
             j <- i to n;
             jj = j * j;
             if (ii - jj) % 2 == 0;
             k <- (i + j) to n)
          yield ii * jj * k
      """,
      params = Array(2, 10, 100),
      minFaster = 30)
  }
}
