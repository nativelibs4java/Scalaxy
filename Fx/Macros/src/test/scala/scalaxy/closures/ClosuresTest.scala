package scalaxy

import org.junit._
import org.junit.Assert._

import scalaxy.fx._

import javafx.event._

class ClosuresTest 
{
  @Test
  def event {
    var called = false
    /*
    val c = closure[ActionListener] {
      println("hehehe")
      called = true
    }
    */
    val c: EventHandler[ActionEvent] = {
      called = true
    }
    c.handle(null)
    assertTrue(called)
  }
}
