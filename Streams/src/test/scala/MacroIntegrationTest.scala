package scalaxy.streams

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
    assertEquals(expectedMessages.infos, actualMessages.infos)
    assertEquals(expectedMessages.warnings.toSet, actualMessages.warnings.toSet)
    assertEquals(expectedMessages.errors, actualMessages.errors)
  }
}

object MacroIntegrationTest
{
  def streamMsg(streamDescription: String, hasPureExpressions: Boolean = false) =
    CompilerMessages(
      infos = List(Streams.optimizedStreamMessage(streamDescription)),
      // TODO investigate why these happen!
      // warnings = Nil)
      warnings = if (hasPureExpressions) List("a pure expression does nothing in statement position; you may be omitting necessary parentheses") else Nil)

  // def ranges = List(
  //   "(0 until n)",
  //   "(1 to 3)",
  //   "(2 until 10 by 2)",
  //   "(20 to 7 by -3)")

  // def intArrays = List(
  //   "Array(1, 2, 3)",
  //   """ "1,2,3".split(",").map(_.toInt) """)

  // def refArrays = List(
  //   "Array((1, 2), (3, 4))",
  //   """ "1,2,3".split(",") """)

  // def intOps = List(
  //   "map(_ * 2)" -> "map",
  //   "filter(_ < 2)" -> "filter",
  //   "filter(_ % 2 == 0)" -> "filter")

  // def mkIntOps(n: Int): Seq[String] =
  //   for (in <- ranges ++ intArrays)

  @Parameters(name = "{0}") def data: java.util.Collection[Array[AnyRef]] = List[(String, CompilerMessages)](

    "Option(1).map(_ * 2).filter(_ < 3)"
      -> streamMsg("Option.map.filter -> Option", hasPureExpressions = true),

    """Option("a").flatMap(v => Option(v).filter(_ != null))"""
      -> streamMsg("Option.flatMap(Option.filter) -> Option", hasPureExpressions = true),

    "(None: Option[String]).map(_ * 2)"
      -> streamMsg("Option.map -> Option", hasPureExpressions = true),

    "Some(1).map(_ * 2).filter(_ < 3)"
      -> streamMsg("Some.map.filter -> Option", hasPureExpressions = true),

    "Seq(0, 1, 2, 3).map(_ * 2).filter(_ < 3)"
      -> streamMsg("Seq.map.filter -> Seq", hasPureExpressions = true),

    "List(0, 1, 2, 3).map(_ * 2).filter(_ < 3)"
      -> streamMsg("List.map.filter -> List", hasPureExpressions = true),

    "Array(1, 2, 3).map(_ * 2).filter(_ < 3)"
      -> streamMsg("Array.map.filter -> Array", hasPureExpressions = true),

    "Array(1, 2, 3).map(_ * 2).filter(_ < 3).toSet"
      -> streamMsg("Array.map.filter -> Set", hasPureExpressions = true),

    "Array(1, 2, 3).map(_ * 2).filter(_ < 3).toList"
      -> streamMsg("Array.map.filter -> List", hasPureExpressions = true),

    "(1 to 3).map(_ * 2).filter(_ < 3).toArray"
      -> streamMsg("Range.map.filter -> Array", hasPureExpressions = true),

    "Array(1, 2, 3).map(_ * 10).sum"
      -> streamMsg("Array.map -> sum", hasPureExpressions = true),

    "Array(1, 2, 3).map(_ * 10).product"
      -> streamMsg("Array.map -> product", hasPureExpressions = true),

    "val n = 10; for (v <- 0 to n) yield v"
      -> streamMsg("Range.map -> IndexedSeq", hasPureExpressions = true),

    "Array(1, 2, 3).map(_ * 2).filterNot(_ < 3)"
      -> streamMsg("Array.map.filterNot -> Array", hasPureExpressions = true),

    "(2 to 10).map(_ * 2).filter(_ < 3)"
      -> streamMsg("Range.map.filter -> IndexedSeq", hasPureExpressions = true),

    "(2 until 10 by 2).map(_ * 2)"
      -> streamMsg("Range.map -> IndexedSeq", hasPureExpressions = true),

    "(20 to 7 by -3).map(_ * 2).filter(_ < 3)"
      -> streamMsg("Range.map.filter -> IndexedSeq", hasPureExpressions = true),

    "Array(1, 2, 3).map(_ * 2).map(_ < 3)"
      -> streamMsg("Array.map.map -> Array", hasPureExpressions = true),

    "(10 to 20).map(i => () => i).map(_())"
      -> streamMsg("Range.map.map -> IndexedSeq", hasPureExpressions = true),

    "(10 to 20).map(_ + 1).map(i => () => i).map(_())"
      -> streamMsg("Range.map.map.map -> IndexedSeq", hasPureExpressions = true),

    "(10 to 20).map(_ * 10).map(i => () => i).reverse.map(_())"
      -> streamMsg("Range.map.map -> IndexedSeq", hasPureExpressions = true),

    "for (p <- (20 until 0 by -2).zipWithIndex) yield p.toString"
      -> streamMsg("Range.zipWithIndex.map -> IndexedSeq", hasPureExpressions = true),

    "for ((v, i) <- (20 until 0 by -2).zipWithIndex) yield (v + i)"
      -> streamMsg("Range.zipWithIndex.withFilter.map -> IndexedSeq", hasPureExpressions = true),

    "Array((1, 2)).map({ case (x, y) => x + y })"
      -> streamMsg("Array.map -> Array", hasPureExpressions = true),

    """Array((1, 2), (3, 4))
        .map(_ match { case p @ (i, j) => (i * 10, j / 10.0) })
        .map({ case (k, l) => k + l })"""
      -> streamMsg("Array.map.map -> Array", hasPureExpressions = true),

    """val n = 10;
      for (i <- 0 to n;
           j <- i to 1 by -1;
           if i % 2 == 1)
        yield { i + j }
    """
      -> streamMsg("Range.flatMap(Range.withFilter.map) -> IndexedSeq", hasPureExpressions = true),

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
      -> streamMsg("Range.flatMap(Range.flatMap(Range.flatMap(Range.map.withFilter.flatMap(Range.map)))) -> IndexedSeq", hasPureExpressions = true),

    """val n = 20;
      for (i <- 0 to n;
           ii = i * i;
           j <- i to n;
           jj = j * j;
           if (ii - jj) % 2 == 0;
           k <- (i + j) to n)
        yield { (ii, jj, k) }
    """
      -> streamMsg("Range.map.flatMap(Range.map.withFilter.flatMap(Range.map)) -> IndexedSeq", hasPureExpressions = true),

    """
      val start = 10
      val end = 20
      (for (i <- start to end by 2) yield
          (() => (i * 2))
      ).map(_())
    """
      -> streamMsg("Range.map.map -> IndexedSeq", hasPureExpressions = true)

  ).map({ case (src, msgs) => Array[AnyRef](src, msgs) })
}
