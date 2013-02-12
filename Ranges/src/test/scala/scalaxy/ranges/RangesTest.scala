package scalaxy.ranges.test

import org.junit._
import org.junit.Assert._

import scala.collection.mutable.ArrayBuffer
import scalaxy.ranges._

class RangesTest 
{
  private val end = 10
  private val start = 3
  private def withBuf(f: ArrayBuffer[Int] => Unit): List[Int] = {
    val buf = ArrayBuffer[Int]()
    f(buf)
    buf.toList
  }
  
  @Test
  def simpleUntil {
    assertEquals(
      withBuf(res => 
        for (i <- start until end) 
          res += (i * 2)),
      withBuf(res => 
        for (i <- start until end optimized) 
          res += (i * 2)))
  }
  
  @Test
  def simpleTo {
    assertEquals(
      withBuf(res => 
        for (i <- start to end) 
          res += (i * 2)),
      withBuf(res => 
        for (i <- start to end optimized) 
          res += (i * 2)))
  }
  
  @Test
  def simpleUntilBy {
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
  def simpleToBy {
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
