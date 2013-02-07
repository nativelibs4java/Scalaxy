package scalaxy

import org.junit._
import org.junit.Assert._

import scalaxy.beans

class BeansTest 
{
  class Bean {
    private var _foo = 0
    def getFoo = _foo
    def setFoo(v: Int) { _foo = v }
    
    private var _bar = 0
    def getBar = _bar
    def setBar(v: Int) { _bar = v }
  }

  @Test
  def simple {
    val b = beans.create[Bean](foo = 10, bar = 12)
    println(s"b = $b")
    assertEquals(10, b.getFoo)
    assertEquals(12, b.getBar)
  }
  
  @Test
  def simple2 {
    val b = beans.create[Bean](10, bar = 12)
    println(s"b = $b")
    assertEquals(10, b.getFoo)
    assertEquals(12, b.getBar)
  }
}
