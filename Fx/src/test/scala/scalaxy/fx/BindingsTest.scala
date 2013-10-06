package scalaxy.fx.test

import org.junit._
import org.junit.Assert._

import scalaxy.fx._

import javafx.beans.Observable
import javafx.beans.value._
import javafx.beans.property._
import javafx.beans.binding._
import javafx.scene.control._

class BindingsTest 
{
  @Test
  def simplePropertyBinding {
    val a: SimpleIntegerProperty = newProperty(2)
    val b = newProperty(3)
    val bb: SimpleIntegerProperty = b
    val c = bind(a.get + b.get)
    
    assertEquals(5, c.get)
    a.set(1)
    b.set(10)
    assertEquals(11, c.get)
  }

  @Test
  def beanPropertyBinding {
    val b = new Button
    val fmt = "Size: %d x %d"
    b.set(minWidth = 10, minHeight = 11)
    val text = bind {
      fmt.format(b.minWidthProperty.intValue, b.minHeightProperty.intValue)
    }
    assertEquals(fmt.format(10, 11), text.get)
    b.set(minWidth = 20)
    assertEquals(fmt.format(20, 11), text.get)
    b.set(minHeight = 21)
    assertEquals(fmt.format(20, 21), text.get)
  }

  @Test
  def beanPropertyBinding2 {
    val b1 = newProperty(10)
    val b2 = bind { 10 + b1.get }
    val b3: IntegerBinding = b2
    
    
    val b = new Button
    val fmt = "Size: %d x %d"
    b.set(
      minWidth = 10, 
      minHeight = 11,
      maxHeight = b.minHeightProperty, // will be bound
      text = bind {
        fmt.format(b.minWidthProperty.intValue, b.minHeightProperty.intValue)
      }
    )
    assertEquals(fmt.format(10, 11), b.getText)
    b.set(minWidth = 20)
    assertEquals(fmt.format(20, 11), b.getText)
    b.set(minHeight = 21)
    assertEquals(fmt.format(20, 21), b.getText)
  }
}
