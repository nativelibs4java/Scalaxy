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
  // scalaxy.streams.impl.veryVerbose = true
  // scalaxy.streams.impl.debug = true
  // scalaxy.streams.impl.quietWarnings = true

  val fnRx = raw".*scala\.Function0\.apply.*"

/*
          def debug(title: String, t: Tree) = new Traverser {
            override def traverse(tree: Tree) = {
              for (s <- Option(tree.symbol); if s != NoSymbol && s.name.toString == "foo") {
                println(s"""
                $title
                  symbol: ${s}
                  owner: ${s.owner}
                  ownerChain: ${ownerChain(s)}
                """)
              }
              super.traverse(tree)
            }
          } traverse t
*/

  @Test
  def foo {
    val src = "(1 to 10).takeWhile(_ < 5).map(_ * 2)"

    import scalaxy.streams.strategy.foolish
    testMessages(src, streamMsg("Range.takeWhile.map -> IndexedSeq"))
  }

  // @Test
  def testTake {
    val src = """
      def f(i: Int) = true;
      (
        (0 to 2).takeWhile(f), Option(1).takeWhile(f), Some(1).takeWhile(f), None.takeWhile(f),
        (0 to 2).dropWhile(f), Option(1).dropWhile(f), Some(1).dropWhile(f), None.dropWhile(f),
        (0 to 2).take(2), Option(1).take(2), Some(1).take(2), None.take(2),
        (0 to 2).drop(2), Option(1).drop(2), Some(1).drop(2), None.drop(2)
      )"""

    {
      import scalaxy.streams.strategy.foolish
      testMessages(src, streamMsg())

    //   // val (_, messages) = compileFast(src)
    //   // println(messages)
    }
  }

  // @Ignore
  // @Test
  def testComp {
    // val src = """(
    //   Some[Any](1).orNull
    // )"""
    val src = """
      val msg = {
        try {
          val foo = 10
          Some(foo)
        } catch {
          case ex: Throwable => None
        }
      } get

      msg
    """

    // assertPluginCompilesSnippetFine(src)
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
      testMessages(src, streamMsg())

    //   // val (_, messages) = compileFast(src)
    //   // println(messages)
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
