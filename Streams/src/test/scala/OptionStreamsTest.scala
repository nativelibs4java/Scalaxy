package scalaxy.streams

package test

import org.junit._
import org.junit.runner.RunWith
import org.junit.runners.Parameterized.Parameters

import scala.collection.JavaConversions._

import IntegrationTests.{streamMsg, potentialSideEffectMsgs}

/** This is just a testbed for "fast" manual tests */
class OptionStreamsTest
    extends StreamComponentsTestBase
    with PerformanceTestBase
    with StreamTransforms
{
  import global._

  scalaxy.streams.impl.verbose = true

  // scalaxy.streams.impl.veryVerbose = true
  // scalaxy.streams.impl.debug = true
  // scalaxy.streams.impl.quietWarnings = true

  @Test
  def testComp2 {
    val options = List(
      "None",
      "(None: Option[Int])",
      "Option[Any](null)",
      "Option[String](null)",
      "Option[String](\"Y\")",
      "Some(0)",
      "Some(\"X\")")
    val suffixes = List(
      None,
      Some("orNull"),
      Some("getOrElse(\"Z\")"),
      Some("get"),
      Some("find(_.contains(\"2\"))"))

    val src = s"""
      def f1(x: Any) = x.toString + "1"
      def f2(x: Any) = x.toString + "2"
      def f3(x: Any) = x.toString + "3"

      List(
        ${{
          for (lhs <- options; rhs <- options; suf <- suffixes) yield {
            val stream = s"$lhs.map(f1).orElse($rhs.map(f2)).map(f3)"
            suf.map(s => stream + "." + s).getOrElse(stream)
          }
        }.mkString(",\n        ")}
      )
    """
    println(src)

    assertMacroCompilesToSameValue(
      src,
      strategy = scalaxy.streams.strategy.foolish)

    // {
    //   import scalaxy.streams.strategy.foolish
    //   testMessages(src, streamMsg("Some.orElse(Some.map).map -> Option"),
    //     expectWarningRegexp = Some(List("there were \\d+ inliner warnings; re-run with -Yinline-warnings for details")))
    // }
  }

}
