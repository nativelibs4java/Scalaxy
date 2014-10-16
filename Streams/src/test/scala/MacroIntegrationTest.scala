package scalaxy.streams

package test

import org.junit._
import org.junit.Assert._
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

import scala.collection.JavaConversions._

@RunWith(classOf[Parallelized])
class MacroIntegrationTest(source: String, expectedMessages: CompilerMessages) extends StreamComponentsTestBase with StreamTransforms {
  @Test def test = {
    assertPluginCompilesSnippetFine(source)

    def filterWarnings(warnings: List[String]) =
      warnings.filterNot(_ == "a pure expression does nothing in statement position; you may be omitting necessary parentheses")

    val actualMessages = assertMacroCompilesToSameValue(
      source, strategy = scalaxy.streams.optimization.aggressive)
    assertEquals(expectedMessages.infos, actualMessages.infos)
    assertEquals(filterWarnings(expectedMessages.warnings).toSet,
      filterWarnings(actualMessages.warnings).toSet)
    assertEquals(expectedMessages.errors, actualMessages.errors)
  }
}

object MacroIntegrationTest
{
  def streamMsg(streamDescription: String) =
    CompilerMessages(
      infos = List(Optimizations.optimizedStreamMessage(streamDescription)))

  scalaxy.streams.impl.verbose = true

  @Parameters(name = "{0}") def data: java.util.Collection[Array[AnyRef]] = List[(String, CompilerMessages)](

    // """{ object Foo { def doit(args: Array[String]) = args.length } ; Foo.doit(Array("1")) }"""
    //   -> CompilerMessages(),

    // "(1 to 10).collect({ case x if x < 5 => x + 1 })"
    //   -> streamMsg("Range.collect -> IndexedSeq")

    "(0 to 10 by 2).exists(_ % 2 == 0)" -> streamMsg("Range.exists"),
    "(0 to 10 by 2).forall(_ % 2 == 0)" -> streamMsg("Range.forall"),
    "(1 to 10 by 2).exists(_ % 2 == 0)" -> streamMsg("Range.exists"),
    "(1 to 10 by 2).forall(_ % 2 == 0)" -> streamMsg("Range.forall"),

    """
      class Foo {
        var col = for (i <- 0 to 10 by 2) yield (() => (i * 3))
        val res = col.map(_())
      }
      new Foo().res
    """
      -> streamMsg("Range.map -> IndexedSeq"),

    """
      class Foo {
        val res = (for (i <- 0 to 10 by 2) yield (() => (i * 3))).map(_())
      }
      new Foo().res
    """
      -> streamMsg("Range.map.map -> IndexedSeq"),

    "def foo = (1 to 10).map(i => () => i * 3).map(_()); foo"
       -> streamMsg("Range.map.map -> IndexedSeq"),

    """
      case class Foo(i: Int)
      val arr = new Array[Foo](5);
      for (Foo(i) <- arr) yield i
    """
      -> streamMsg("Array.withFilter.map -> Array"),

    /// Range.takeWhile and .dropWhile return a Range, which doesn't fit nicely with WhileOps.
    "(1 to 10).takeWhile(_ < 5)" -> CompilerMessages(),
    "(1 to 10).dropWhile(_ < 5)" -> CompilerMessages(),

    "(1 to 10).takeWhile(_ < 5).map(_ * 2)"
      -> streamMsg("Range.takeWhile.map -> IndexedSeq"),

    "(1 to 10).dropWhile(_ < 5).map(_ * 2)"
      -> streamMsg("Range.dropWhile.map -> IndexedSeq"),

    "(1 to 10).map(_ * 2).toSet.toList"
      -> streamMsg("Range.map -> Set"),

    "(1 to 10).map(_ * 2).toVector.toList"
      -> streamMsg("Range.map.toVector -> List"),

    "List(1, 2, 2, 3).toVector.map((_: Int) * 2).toList"
      -> streamMsg("List.toVector.map -> List"),

    "Array((1, 2), (3, 4), (5, 6)) find (_._1 > 1) map (_._2)"
      -> streamMsg("Array.find.map -> Option"),

    "(1 to 10).count(_ < 5)"
      -> streamMsg("Range.count"),

    "List(1, 2, 3).flatMap(v => List(v * 2, v * 2 + 1)).count(_ % 2 == 0)"
      -> streamMsg("List.flatMap(List).count"),

    "List(1, 2, 3).flatMap(v => List(v * 2, v * 2 + 1).map(_ + 1)).count(_ % 2 == 0)"
      -> streamMsg("List.flatMap(List.map).count"),

    "Array(1, 2, 3, 4).flatMap(v => Array(v, v * 2).find(_ > 2))"
      -> streamMsg("Array.flatMap(Array.find) -> Array"),

    "Array(1, 2, 3, 4).flatMap(v => List(v, v * 2).map(_ + 1)).find(_ > 2)"
      -> streamMsg("Array.flatMap(List.map).find -> Option"),

    "Option(10).map(_ * 2).getOrElse(10)"
      -> streamMsg("Option.map.getOrElse"),

    "Option(10).filter(_ < 5).isEmpty"
      -> streamMsg("Option.filter.isEmpty"),

    "Some(10).map(_ * 2).get"
      -> streamMsg("Some.map.get"),

    "List(1, 2, 3).map(_ * 2)"
      -> streamMsg("List.map -> List"),

    "Array(1, 2, 3).flatMap { case 1 => Some(10) case v => Some(v * 100) }"
      -> streamMsg("Array.flatMap -> Array"),

    "val n = 3; (1 to n) map (_ * 2)"
      -> streamMsg("Range.map -> IndexedSeq"),

    "val n = 3; (1 to n).toList map (_ * 2)"
      -> streamMsg("Range.toList.map -> List"),

    "val n = 3; (1 to n).toArray map (_ * 2)"
      -> streamMsg("Range.toArray.map -> Array"),

    "Option(1).map(_ * 2).filter(_ < 3)"
      -> streamMsg("Option.map.filter -> Option"),

    """Option("a").flatMap(v => Option(v).filter(_ != null))"""
      -> streamMsg("Option.flatMap(Option.filter) -> Option"),

    "(None: Option[String]).map(_ * 2)"
      -> streamMsg("Option.map -> Option"),

    "Some(1).map(_ * 2).filter(_ < 3)"
      -> streamMsg("Some.map.filter -> Option"),

    "Seq(0, 1, 2, 3).map(_ * 2).filter(_ < 3)"
      -> streamMsg("Seq.map.filter -> Seq"),

    "List(0, 1, 2, 3).map(_ * 2).filter(_ < 3)"
      -> streamMsg("List.map.filter -> List"),

    "Array(1, 2, 3).map(_ * 2).filter(_ < 3)"
      -> streamMsg("Array.map.filter -> Array"),

    "Array(1, 2, 3).map(_ * 2).filter(_ < 3).toSet"
      -> streamMsg("Array.map.filter -> Set"),

    "Array(1, 2, 3).map(_ * 2).filter(_ < 3).toList"
      -> streamMsg("Array.map.filter -> List"),

    "Array(1, 2, 3).map(_ * 2).filter(_ < 3).toVector"
      -> streamMsg("Array.map.filter -> Vector"),

    "{ val list = List(1, 2, 3); list.map(_ * 2).filter(_ < 3) }"
      -> streamMsg("List.map.filter -> List"),

    "(Nil: scala.collection.immutable.List[Int]).map(_ * 2).filter(_ < 3).toArray"
      -> streamMsg("List.map.filter -> Array"),

    "(1 :: 2 :: 3 :: Nil).map(_ * 2).filter(_ < 3).toArray"
      -> streamMsg("List.map.filter -> Array"),

    "(1 to 3).map(_ * 2).filter(_ < 3).toArray"
      -> streamMsg("Range.map.filter -> Array"),

    "(1 to 3).map(_ + 1).sum"
      -> streamMsg("Range.map -> sum"),

    "Array(1, 2, 3).map(_ * 10).sum"
      -> streamMsg("Array.map -> sum"),

    "Array(1, 2, 3).map(_ * 10).product"
      -> streamMsg("Array.map -> product"),

    "val n = 10; for (v <- 0 to n) yield v"
      -> streamMsg("Range.map -> IndexedSeq"),

    "Array(1, 2, 3).map(_ * 2).filterNot(_ < 3)"
      -> streamMsg("Array.map.filterNot -> Array"),

    "(2 to 10).map(_ * 2).filter(_ < 3)"
      -> streamMsg("Range.map.filter -> IndexedSeq"),

    "(2 until 10 by 2).map(_ * 2)"
      -> streamMsg("Range.map -> IndexedSeq"),

    "(20 to 7 by -3).map(_ * 2).filter(_ < 3)"
      -> streamMsg("Range.map.filter -> IndexedSeq"),

    "Array(1, 2, 3).map(_ * 2).map(_ < 3)"
      -> streamMsg("Array.map.map -> Array"),

    "(10 to 20).map(i => () => i).map(_())"
      -> streamMsg("Range.map.map -> IndexedSeq"),

    "(10 to 20).map(_ + 1).map(i => () => i).map(_())"
      -> streamMsg("Range.map.map.map -> IndexedSeq"),

    "(10 to 20).map(_ * 10).map(i => () => i).reverse.map(_())"
      -> streamMsg("Range.map.map -> IndexedSeq"),

    "for (p <- (20 until 0 by -2).zipWithIndex) yield p.toString"
      -> streamMsg("Range.zipWithIndex.map -> IndexedSeq"),

    "for ((v, i) <- (20 until 0 by -2).zipWithIndex) yield (v + i)"
      -> streamMsg("Range.zipWithIndex.withFilter.map -> IndexedSeq"),

    "Array((1, 2)).map({ case (x, y) => x + y })"
      -> streamMsg("Array.map -> Array"),

    """Array((1, 2), (3, 4))
        .map(_ match { case p @ (i, j) => (i * 10, j / 10.0) })
        .map({ case (k, l) => k + l })"""
      -> streamMsg("Array.map.map -> Array"),

    """
      val col: List[Int] = (0 to 2).toList;
      col.filter(v => (v % 2) == 0).map(_ * 2)
    """
      -> streamMsg("List.filter.map -> List"),

    """val n = 10;
      for (i <- 0 to n;
           j <- i to 1 by -1;
           if i % 2 == 1)
        yield { i + j }
    """
      -> streamMsg("Range.flatMap(Range.withFilter.map) -> IndexedSeq"),

    """val n = 10;
      for (i <- 0 to n;
           j <- i to 0 by -1)
        yield { i + j }
    """
      -> streamMsg("Range.flatMap(Range.map) -> IndexedSeq"),

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
      -> streamMsg("Range.map.flatMap(Range.map.withFilter.flatMap(Range.map)) -> IndexedSeq"),

    """
      val n = 5
      for (i <- 0 until n; v <- (i to 0 by -1).toArray; j <- 0 until v) yield {
        (i, v, j)
      }
    """
      -> streamMsg("Range.flatMap(Range.toArray.flatMap(Range.map)) -> IndexedSeq"),

    """
      val start = 10
      val end = 20
      (for (i <- start to end by 2) yield
          (() => (i * 2))
      ).map(_())
    """
      -> streamMsg("Range.map.map -> IndexedSeq")

  ).map({ case (src, msgs) => Array[AnyRef](src, msgs) })
}
