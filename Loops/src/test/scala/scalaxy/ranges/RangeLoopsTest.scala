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
