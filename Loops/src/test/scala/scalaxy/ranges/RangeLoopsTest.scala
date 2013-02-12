package scalaxy.loops.test

import org.junit._
import org.junit.Assert._

import scala.collection.mutable.ArrayBuffer
import scalaxy.loops._

class LoopsTest 
{
  private val end = 10
  private val start = 3
  private def withBuf(f: ArrayBuffer[Int] => Unit): List[Int] = {
    val buf = ArrayBuffer[Int]()
    f(buf)
    buf.toList
  }
  
  @Test
  def nestedRanges {
    val n = 10
    val m = 3
    val o = 5
    val p = 2
    assertEquals(
      withBuf(res =>
        for (i <- 0 until n)
          for (j <- 0 until m)
            for (k <- 0 until o)
              for (l <- 0 until p)
                res += (i * 1 + j * 10 + k * 100 + l * 1000) / 10),
      withBuf(res =>
        for (i <- 0 until n optimized)
          for (j <- 0 until m optimized)
            for (k <- 0 until o optimized)
              for (l <- 0 until p optimized)
                res += (i * 1 + j * 10 + k * 100 + l * 1000) / 10))
  }
  
  @Test
  def simpleRangeUntil {
    assertEquals(
      withBuf(res => 
        for (i <- start until end) 
          res += (i * 2)),
      withBuf(res => 
        for (i <- start until end optimized) 
          res += (i * 2)))
  }
  
  @Test
  def simpleRangeTo {
    assertEquals(
      withBuf(res => 
        for (i <- start to end) 
          res += (i * 2)),
      withBuf(res => 
        for (i <- start to end optimized) 
          res += (i * 2)))
  }
  
  @Test
  def simpleRangeUntilBy {
    assertEquals(
      withBuf(res => 
        for (i <- start until end by 2) 
          res += (i * 2)),
      withBuf(res => 
        for (i <- start until end by 2 optimized) 
          res += (i * 2)))
    // This should be empty:
    assertEquals(
      Nil,
      withBuf(res => 
        for (i <- start until end by -2 optimized) 
          res += (i * 2)))
    assertEquals(
      withBuf(res => 
        for (i <- end until start by -2) 
          res += (i * 2)),
      withBuf(res => 
        for (i <- end until start by -2 optimized) 
          res += (i * 2)))
  }
  
  @Test
  def simpleRangeToBy {
    assertEquals(
      withBuf(res => 
        for (i <- start to end by 2) 
          res += (i * 2)),
      withBuf(res => 
        for (i <- start to end by 2 optimized) 
          res += (i * 2)))
    // This should be empty:
    assertEquals(
      Nil,
      withBuf(res => 
        for (i <- start to end by -2 optimized) 
          res += (i * 2)))
    assertEquals(
      withBuf(res => 
        for (i <- end to start by -2) 
          res += (i * 2)),
      withBuf(res => 
        for (i <- end to start by -2 optimized) 
          res += (i * 2)))
  }
}
