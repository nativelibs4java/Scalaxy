package scalaxy

import org.junit._
import org.junit.Assert._

import scalaxy.beans._

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
    def getB() = _b
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
    val bean = new Bean().set(
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
    assertEquals(a, new Bean().set(a = a).getA)
    assertEquals(b, new Bean().set(a = b).getA)
    assertEquals(b, new Bean().set(b = b).getB)
  }
  
  @Test
  def child {
    val child = new Bean().set(bar = 12)
    assertEquals(child, new Bean().set(child = child).getChild)
    assertEquals(123, new Bean().set(child = new Bean().set(foo = 123)).getChild.getFoo)
  }
  
  @Test
  def mutableScala {
    assertEquals(10, new Mutable().set(x = 10).x)
  }
}
