package scalaxy.streams

package test

import org.junit._
import org.junit.Assert._

class StreamsTest extends StreamComponentsTestBase with StreamTransforms {
  import global._

  @Test
  def testArrayMapMapFilterMap {
    val SomeStream(Stream(ArrayStreamSource(_, _), ops, CanBuildFromSink(_))) = typeCheck(q"""
      Array(1).map(_ + 1).map(_ * 10).filter(_ < 10)
    """)
    // println(s"ops = " + ops.mkString("\n\t"))
    val MapOp(_, _, _) :: MapOp(_, _, _) :: FilterOp(_, _, false, "filter") :: Nil = ops
  }

  @Test
  def testRangeMapMapFilterMap {
    val SomeStream(Stream(InlineRangeStreamSource(_, _, 2, true, _), ops, CanBuildFromSink(_))) = typeCheck(q"""
      (1 to 10 by 2).map(_ + 1).map(_ * 10).filter(_ < 10)
    """)
    // println(s"ops = " + ops.mkString("\n\t"))
    val MapOp(_, _, _) :: MapOp(_, _, _) :: FilterOp(_, _, false, "filter") :: Nil = ops
  }

  @Test
  def testFlatMap {
    val SomeStream(Stream(source, ops, sink)) = typeCheck(q"""
      for (a <- Array(Array(1)); len = a.length; v <- a) yield (a, len, v)
    """)
    // println(s"ops = " + ops.mkString("\n\t"))
  }

  @Test
  def testMaps {
    val SomeStream(Stream(source, ops, sink)) = typeCheck(q"""
      for ((a, i) <- Array(Array(1)).zipWithIndex; len = a.length; if len < i) {
        println(a + ", " + len + ", " + i)
      }
    """)
    // println(s"ops = " + ops.mkString("\n\t"))
  }

  // @Test
  // def testNest {
  //   val x = q"""
  //     ((x$2: (Int, Int)) => (x$2: (Int, Int) @unchecked) match {
  //       case (_1: Int, _2: Int)(Int, Int)((j @ _), (jj @ _)) => scala.this.Predef.intWrapper(i.+(j)).to(Example2.this.n).map[(Int, Int, Int), scala.collection.immutable.IndexedSeq[(Int, Int, Int)]](((k: Int) => scala.Tuple3.apply[Int, Int, Int](ii, jj, k)))(immutable.this.IndexedSeq.canBuildFrom[(Int, Int, Int)])
  //     })
  //   """
  // }
}
