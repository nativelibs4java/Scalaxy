package scalaxy.fx

import javafx.event._

class FunctionEventHandler[E <: Event](f: E => Unit) extends EventHandler[E] {
  override def handle(event: E) {
    f(event)
  }
}
