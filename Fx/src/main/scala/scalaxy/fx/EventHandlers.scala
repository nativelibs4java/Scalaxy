package scalaxy.fx

import scala.language.implicitConversions

import javafx.event.{ Event, EventHandler }

import scala.language.experimental.macros

/** Meant to be imported by (package) objects that want to expose event handler macros. */
private[fx] trait EventHandlers
{
  /** Implicit conversion from an event handler function to a JavaFX EventHandler[_]. */
  implicit def functionHandler[E <: Event](f: E => Unit): EventHandler[E] =
    macro impl.EventHandlerMacros.functionHandler[E]

  /** Implicit conversion from an event handler block to a JavaFX EventHandler[_]. */
  implicit def blockHandler[E <: Event](block: Unit): EventHandler[E] =
    macro impl.EventHandlerMacros.blockHandler[E]
}
