package scalaxy.streams

package test

import SideEffectSeverity._

import org.junit._
import org.junit.Assert._

class StreamsTest extends StreamComponentsTestBase with StreamTransforms {
  import global._

  @Test
  def testFindSink {
    assertEquals(Some(ArrayOpsSink), SomeStream.findSink(List(ArrayOpsOp)))
    assertEquals(Some(VectorBuilderSink), SomeStream.findSink(List(ArrayOpsOp, VectorBuilderSink)))
    assertEquals(None, SomeStream.findSink(List(ArrayOpsOp, FilterOp(null, false, null))))
    // val Some(CanBuildFromSink(null)) = SomeStream.findSink(List(ListBufferSink, ZipWithIndexOp(null)))
    assertEquals(Some(ListBufferSink), SomeStream.findSink(List(ArrayBuilderSink, ListBufferSink)))
  }

  @Test
  def testArrayMapMapFilterMap {
    val SomeStream(Stream(ArrayStreamSource(_, _, _), ops, ArrayBuilderSink, false)) = typecheck(q"""
      Array(1).map(_ + 1).map(_ * 10).filter(_ < 10)
    """)
    val List(ArrayOpsOp, MapOp(_, _), ArrayOpsOp, MapOp(_, _), ArrayOpsOp, FilterOp(_, false, "filter")) = ops
  }

  @Test
  def testArrayMap {
    val SomeStream(Stream(ArrayStreamSource(_, _, _), ops, ArrayBuilderSink, false)) = typecheck(q"""
      Array(1).map(_ + 1)
    """)
    val List(ArrayOpsOp, MapOp(_, _)) = ops
  }

  @Test
  def testListMap {
    val SomeStream(s) = typecheck(q"""
      List(1).map(_ + 1)
    """)
    // Inline list creation is rewritten to an array.
    val Stream(ArrayStreamSource(_, _, _), ops, CanBuildFromSink(_), false) = s
    val List(MapOp(_, _)) = ops
  }

  @Test
  def testRangeMapMapFilterMap {
    val SomeStream(Stream(InlineRangeStreamSource(_, _, 2, true, _), ops, CanBuildFromSink(_), false)) = typecheck(q"""
      (1 to 10 by 2).map(_ + 1).map(_ * 10).filter(_ < 10)
    """)
    val List(MapOp(_, _), MapOp(_, _), FilterOp(_, false, "filter")) = ops
  }

  @Test
  def testFlatMap {
    val SomeStream(Stream(source, ops, sink, false)) = typecheck(q"""
      for (a <- Array(Array(1)); len = a.length; v <- a) yield (a, len, v)
    """)
  }

  @Test
  def testToVector {
    val tree = typecheck(q"""
      Array(1, 2, 3).map(_ + 1).toVector
    """)
    val SomeStream(Stream(source, ops, sink, _)) = tree
    val List(ArrayOpsOp, MapOp(_, _), ArrayOpsOp) = ops
    val VectorBuilderSink = sink
  }

  @Test
  def testMaps {
    val SomeStream(Stream(source, ops, sink, _)) = typecheck(q"""
      for ((a, i) <- Array(Array(1)).zipWithIndex; len = a.length; if len < i) {
        println(a + ", " + len + ", " + i)
      }
    """)
  }

  @Test
  def testBasicSideEffects {
    val SomeStream(stream) = typecheck(q"""
      (0 to 10).map(i => { println(i); i }).map(println)
    """)
    assertEquals(List(Unsafe, Unsafe), stream.closureSideEffectss.flatten.map(_.severity))

    val SomeStream(stream2) = typecheck(q"""
      (0 to 10).map(i => { new Object().toString + i })
    """)
    assertEquals(List(Unsafe), stream2.closureSideEffectss.flatten.map(_.severity))
  }
}
