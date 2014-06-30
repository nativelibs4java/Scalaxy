package scalaxy.fx.test

import scala.language.reflectiveCalls

import org.junit._
import org.junit.Assert._

import scalaxy.fx._

import javafx.beans.Observable
import javafx.beans.value._
import javafx.beans.property._
import javafx.beans.binding._
import javafx.scene.control._

class BindingsTest {
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
    assertEquals(fmt.format(10, 11), text.getValue)
    b.set(minWidth = 20)
    assertEquals(fmt.format(20, 11), text.getValue)
    b.set(minHeight = 21)
    assertEquals(fmt.format(20, 21), text.getValue)
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

  @Test
  def propertyCreationAndConversionTest {
    {
      val p: SimpleIntegerProperty = newProperty(10)
      val v: Int = p;
      assertEquals(10, v);
    }

    {
      val p: SimpleLongProperty = newProperty(10L)
      val v: Long = p;
      assertEquals(10L, v);
    }

    {
      val p: SimpleFloatProperty = newProperty(10.0f)
      val v: Float = p;
      assertEquals(10.0f, v, 0);
    }

    {
      val p: SimpleDoubleProperty = newProperty(10.0)
      val v: Double = p;
      assertEquals(10.0, v, 0);
    }

    {
      val p: SimpleBooleanProperty = newProperty(true)
      val v: Boolean = p;
      assertEquals(true, v);
    }
  }
}
