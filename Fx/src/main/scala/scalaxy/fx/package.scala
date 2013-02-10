package scalaxy

import javafx.beans._
import javafx.beans.value._
import javafx.beans.property._
import javafx.beans.binding._
import javafx.event._
import javafx.scene._

import scala.language.dynamics
import scala.language.experimental.macros
import scala.reflect.NameTransformer
import scala.reflect.macros.Context

package object fx
    extends BeanExtensions
    with Bindings
    with GenericTypes
    with EventHandlers
    with ObservableValueExtensions

