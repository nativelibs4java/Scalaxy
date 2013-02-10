package scalaxy.fx

import javafx.beans.property._
import javafx.beans.value._
import javafx.beans.binding._

// This trait is just here to play with the typer: it has no implementation.
sealed trait GenericType[T, J, B <: Binding[J], P <: Property[J]]
