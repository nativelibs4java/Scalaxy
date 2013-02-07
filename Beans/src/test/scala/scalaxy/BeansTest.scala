package scalaxy

import org.junit._
import org.junit.Assert._

import scalaxy.beans

class BeansTest 
{
  class A
  class B extends A
  class Bean {
    private var _foo = 0
    def getFoo = _foo
    def setFoo(foo: Int) { _foo = foo }
    
    private var _bar = 0.0
    def getBar = _bar
    def setBar(bar: Double) { _bar = bar }
    
    private var _a: A = _
    def getA = _a
    def setA(a: A) { _a = a }
    
    private var _b: B = _
    def getB = _b
    def setB(b: B) { _b = b }
    
    private var _child: Bean = _
    def getChild = _child
    def setChild(child: Bean) { _child = child }
  }
  class Mutable {
    var x: Int = _
    var y: Double = _
    var a: A = _
    var b: B = _
  }

  @Test
  def simple {
    val bean = beans.create[Bean](
      bar = 12,
      foo = 10
    )
    assertEquals(10, bean.getFoo)
    assertEquals(12, bean.getBar, 0)
  }
  
  @Test
  def inheritance {
    val a = new A
    val b = new B
    assertEquals(a, beans.create[Bean](a = a).getA)
    assertEquals(b, beans.create[Bean](a = b).getA)
    assertEquals(b, beans.create[Bean](b = b).getB)
  }
  
  @Test
  def child {
    val child = beans.create[Bean](bar = 12)
    assertEquals(child, beans.create[Bean](child = child).getChild)
    assertEquals(123, beans.create[Bean](child = beans.create[Bean](foo = 123)).getChild.getFoo)
  }
  
  @Test
  def mutableScala {
    assertEquals(10, beans.create[Mutable](x = 10).x)
  }
}
