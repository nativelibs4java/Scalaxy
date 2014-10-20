package scalaxy.streams

package test

import org.junit._
import org.junit.runner.RunWith
import org.junit.runners.Parameterized.Parameters

import scala.collection.JavaConversions._

import IntegrationTests.streamMsg

class StrategyTest extends StreamComponentsTestBase with StreamTransforms {
  scalaxy.streams.impl.verbose = true
  scalaxy.streams.impl.veryVerbose = false

  @Ignore
  @Test
  def testPrintlns {
    val src = """
      (0 to 10).map(i => { println(i); i }).map(_ * 2).map(i => { println(i); i })
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
      testMessages(src, streamMsg("Range.map.map.map -> IndexedSeq"))
    }

    {
      import scalaxy.streams.strategy.foolish
      testMessages(src, streamMsg("Range.map.map.map -> IndexedSeq"))
    }
  }
  @Ignore
  @Test
  def testProbablySafe {
    val src = """
      import language.reflectiveCalls
      type Tpe = { def +++(rhs: Any): Any }
      def f(v: List[Tpe]) = v.map(_ +++ "ha!").filter(_.toString == null).map(_.hashCode)
    """

    {
      import scalaxy.streams.strategy.safer
      testMessages(src, streamMsg("List.map -> List", "List.filter -> List", "List.map -> List"))
    }

    {
      import scalaxy.streams.strategy.safe
      testMessages(src, streamMsg("List.map.filter.map -> List"))
    }
  }
}
