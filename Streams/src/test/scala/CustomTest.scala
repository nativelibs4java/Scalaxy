package scalaxy.streams

package test

import org.junit._
import org.junit.runner.RunWith
import org.junit.runners.Parameterized.Parameters

import scala.collection.JavaConversions._

import IntegrationTests.{streamMsg, potentialSideEffectMsgs}

class CustomTest extends StreamComponentsTestBase with StreamTransforms {
  scalaxy.streams.impl.verbose = true
  scalaxy.streams.impl.veryVerbose = false

  // import scalaxy.streams.strategy.default
  // import scalaxy.streams.strategy.foolish


      // print((0 to 10).toList)
      // print((0 to 10).map(outsider))
      // print((0 to 10).map(outsider).map(_ + 2))
  
  // @Ignore

  // @Ignore
  @Test
  def testPrints {
    val src = """
      (0 to 1).map(i => { print(i); i }).map(_ * 2).map(i => { print(i); i })
      println()
    """

    {
      import scalaxy.streams.strategy.safer
      testMessages(src, streamMsg("Range.map.map -> IndexedSeq"))
    }

    {
      import scalaxy.streams.strategy.safe
      testMessages(src, streamMsg("Range.map.map -> IndexedSeq"))
    }

    {
      import scalaxy.streams.strategy.aggressive
      testMessages(src, streamMsg("Range.map.map.map -> IndexedSeq").
        copy(warnings = potentialSideEffectMsgs("scala.Predef.print")))
    }

    {
      import scalaxy.streams.strategy.foolish
      testMessages(src, streamMsg("Range.map.map.map -> IndexedSeq").
        copy(warnings = potentialSideEffectMsgs("scala.Predef.print")))
    }
  }

  @Ignore
  @Test
  def test {
    import scalaxy.streams.strategy.default
    testMessages(
      """
        val n = 10; for (i <- 0 to n; j <- i to 0 by -1) yield { i + j }
        
        // val n = 10
        // val start = 0
        // val end = 100

        // import scala.collection.mutable.ArrayBuffer

        // private def withBuf[T : ClassTag](f: ArrayBuffer[T] => Unit): List[T] = {
        //   val buf = ArrayBuffer[T]()
        //   f(buf)
        //   buf.toList
        // }

        // withBuf[() => Int](res =>
        //   optimize {
        //     for (i <- start to end by 2)
        //       res += (() => (i * 2))
        //   }
        // )



       // (0 until n).toList
  //
        // (0 until n).dropWhile(x => x < n / 2).toSeq
      // List(0, 1, 2).map(_ + 1).toSeq

       // (0 until n).filter(v => (v % 2) == 0).map(_ * 2).toArray.toSeq

  //      for (v <- Array(1, 2, 3)) yield v + 1
      """,
      streamMsg("Array.map -> Array")
    )
  }
}
