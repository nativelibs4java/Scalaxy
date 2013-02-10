package scalaxy.fx

import scala.language.experimental.macros
import scala.reflect.macros.Context

import javafx.beans._
import javafx.beans.property._
import javafx.beans.value._
import javafx.beans.binding._
import javafx.event._

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
