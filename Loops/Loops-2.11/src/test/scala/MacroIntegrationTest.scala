package scalaxy.loops

package test

import org.junit._
import org.junit.Assert._
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

import scala.collection.JavaConversions._

@RunWith(classOf[Parameterized])
class MacroIntegrationTest(source: String, expectedMessages: CompilerMessages) extends StreamComponentsTestBase with Streams {
  @Test def test = {
    val actualMessages = assertMacroCompilesToSameValue(source)
    assertEquals(expectedMessages, actualMessages)
  }
}

object MacroIntegrationTest
{
  def streamMsg(streamDescription: String) =
    CompilerMessages(infos = List(impl.optimizedStreamMessage(streamDescription)))

  @Parameters(name = "{0}") def data: java.util.Collection[Array[AnyRef]] = List[(String, CompilerMessages)](
    "Array(1, 2, 3).map(_ * 2).filter(_ < 3)" -> streamMsg("Array.map.filter -> Array"),
    "Array(1, 2, 3).map(_ * 2).filterNot(_ < 3)" -> streamMsg("Array.map.filterNot -> Array"),
    "(2 to 10).map(_ * 2).filter(_ < 3)" -> streamMsg("Range.map.filter -> IndexedSeq"),
    "(2 until 10 by 2).map(_ * 2)" -> streamMsg("Range.map -> IndexedSeq"),
    "(20 to 7 by -3).map(_ * 2).filter(_ < 3)" -> streamMsg("Range.map.filter -> IndexedSeq"),
    "Array(1, 2, 3).map(_ * 2).map(_ < 3)" -> streamMsg("Array.map.map -> Array"),
    "(10 to 20).map(i => () => i).map(_())" -> streamMsg("Range.map.map -> IndexedSeq"),
    "(10 to 20).map(_ + 1).map(i => () => i).map(_())" -> streamMsg("Range.map.map.map -> IndexedSeq"),
    "(10 to 20).map(_ * 10).map(i => () => i).reverse.map(_())" -> streamMsg("Range.map.map -> IndexedSeq"),
    "for ((v, i) <- (20 until 0 by -2).zipWithIndex) yield (v + i)" -> streamMsg("Range.zipWithIndex.map -> IndexedSeq"),
    """
      Array((1, 2), (3, 4))
        .map(_ match { case p @ (i, j) => (i * 10, j / 10.0) })
        .map({ case (k, l) => k + l })
    """ -> streamMsg("Array.map.map -> Array")
  ).map({ case (src, msgs) => Array[AnyRef](src, msgs) })
}
