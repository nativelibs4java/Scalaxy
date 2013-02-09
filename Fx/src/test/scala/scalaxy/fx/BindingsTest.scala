package scalaxy

import org.junit._
import org.junit.Assert._

import scalaxy.fx._

import javafx.beans.value._
import javafx.beans.property._
import javafx.beans.binding._

class BindingsTest 
{
  @Test
  def handlerBlock {
    val a = new SimpleIntegerProperty
    val b = new SimpleIntegerProperty
    val c = bind(a.get + b.get)
    
    assertEquals(0, c())
    a.set(1)
    b.set(10)
    assertEquals(11, c())
  }
}
