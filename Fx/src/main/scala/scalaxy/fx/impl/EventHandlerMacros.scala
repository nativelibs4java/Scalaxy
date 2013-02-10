package scalaxy.fx
package impl

import javafx.event._

import scala.language.experimental.macros
import scala.reflect.macros.Context

private[fx] object EventHandlerMacros
{
  def functionHandler[E <: Event]
      (c: Context)
      (f: c.Expr[E => Unit])
      (implicit e: c.WeakTypeTag[E]): c.Expr[EventHandler[E]] =
  {
    c.universe.reify(
      new EventHandler[E] {
        override def handle(event: E) {
          f.splice(event)
        }
      }
    )
  }

  def blockHandler[E <: Event]
      (c: Context)
      (block: c.Expr[Unit])
      (implicit e: c.WeakTypeTag[E]): c.Expr[EventHandler[E]] =
  {
    c.universe.reify(
      new EventHandler[E] {
        override def handle(event: E) {
          block.splice
        }
      }
    )
  }
}
