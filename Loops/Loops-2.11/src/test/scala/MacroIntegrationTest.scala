package scalaxy.loops

package test

import org.junit._
import org.junit.Assert._
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

import scala.collection.JavaConversions._

@RunWith(classOf[Parameterized])
class MacroIntegrationTest(source: String, expectedMessages: CompilerMessages) extends StreamComponentsTestBase with StreamTransforms {
  @Test def test = {
    val actualMessages = assertMacroCompilesToSameValue(source)
    assertEquals(expectedMessages, actualMessages)
  }
}

object MacroIntegrationTest
{
  def streamMsg(streamDescription: String, pureExpressions: Int = 0) =
    CompilerMessages(
      infos = List(impl.optimizedStreamMessage(streamDescription)),
      // TODO investigate why these happen!
      warnings = (1 to pureExpressions).toList.map(_ =>
        "a pure expression does nothing in statement position; you may be omitting necessary parentheses"))

  @Parameters(name = "{0}") def data: java.util.Collection[Array[AnyRef]] = List[(String, CompilerMessages)](

    "Array(1, 2, 3).map(_ * 2).filter(_ < 3)"
      -> streamMsg("Array.map.filter -> Array", pureExpressions = 1),

    "Array(1, 2, 3).map(_ * 2).filterNot(_ < 3)"
      -> streamMsg("Array.map.filterNot -> Array", pureExpressions = 1),

    "(2 to 10).map(_ * 2).filter(_ < 3)"
      -> streamMsg("Range.map.filter -> IndexedSeq"),

    "(2 until 10 by 2).map(_ * 2)"
      -> streamMsg("Range.map -> IndexedSeq"),

    "(20 to 7 by -3).map(_ * 2).filter(_ < 3)"
      -> streamMsg("Range.map.filter -> IndexedSeq"),

    "Array(1, 2, 3).map(_ * 2).map(_ < 3)"
      -> streamMsg("Array.map.map -> Array", pureExpressions = 1),

    "(10 to 20).map(i => () => i).map(_())"
      -> streamMsg("Range.map.map -> IndexedSeq"),

    "(10 to 20).map(_ + 1).map(i => () => i).map(_())"
      -> streamMsg("Range.map.map.map -> IndexedSeq"),

    "(10 to 20).map(_ * 10).map(i => () => i).reverse.map(_())"
      -> streamMsg("Range.map.map -> IndexedSeq"),

    "for (p <- (20 until 0 by -2).zipWithIndex) yield p.toString"
      -> streamMsg("Range.zipWithIndex.map -> IndexedSeq", pureExpressions = 1),

    "for ((v, i) <- (20 until 0 by -2).zipWithIndex) yield (v + i)"
      -> streamMsg("Range.zipWithIndex.withFilter.map -> IndexedSeq", pureExpressions = 1),

    """Array((1, 2), (3, 4))
        .map(_ match { case p @ (i, j) => (i * 10, j / 10.0) })
        .map({ case (k, l) => k + l })"""
      -> streamMsg("Array.map.map -> Array", pureExpressions = 3),

    """val n = 10;
      for (i <- 0 to n;
           j <- i to 1 by -1;
           if i % 2 == 1)
        yield { i + j }
    """
      -> streamMsg("Range.flatMap(Range.withFilter.map) -> IndexedSeq"),

    """val n = 20;
      for (i <- 0 to n;
           j <- i to n;
           k <- j to n;
           l <- k to n;
           sum = i + j + k + l;
           if sum % 3 == 0;
           m <- l to n)
        yield { sum * m }
    """
      -> streamMsg("Range.flatMap(Range.flatMap(Range.flatMap(Range.map.withFilter.flatMap(Range.map)))) -> IndexedSeq"),

    """val n = 20;
      for (i <- 0 to n;
           ii = i * i;
           j <- i to n;
           jj = j * j;
           if (ii - jj) % 2 == 0;
           k <- (i + j) to n)
        yield { (ii, jj, k) }
    """
      -> streamMsg("Range.map.flatMap(Range.map.withFilter.flatMap(Range.map)) -> IndexedSeq")

  ).map({ case (src, msgs) => Array[AnyRef](src, msgs) })
}
