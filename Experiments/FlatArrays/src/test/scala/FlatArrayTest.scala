package scalaxy.flatarray
package test

import org.junit._
import scala.language.dynamics

import scala.collection.JavaConversions._

class FlatArrayTest
{
  case class Foo(
      a: Int,
      b: Float,
      c: Double,
      pt: (Double, Double))
  {
    /// This can't be optimized away: it causes materialization of the class
    def sum = a + b
    
    
    /// Ensures this is not instantiated!
    ???
  }
  
  
  implicit val FooIO: FlatIO[Foo] = FlatIO.of[Foo]
  
  def sumOptimized(foo: Ghost[Foo]) = foo.a + foo.b
  
  @Test
  def test {
    val n = 100
    val in: FlatArray[Foo] = new FlatArray[Foo](n)
    
    val out = new Array[Float](n)
    for (i <- 0 until n) {
      val item = in(i)
      
      out(i) = item.a + item.b
    }
  }
}
