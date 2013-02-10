package scalaxy.fx
package impl

import javafx.beans._
import javafx.beans.binding._
import javafx.beans.property._
import javafx.beans.value._
import javafx.event._

import scala.language.experimental.macros
import scala.reflect.macros.Context

/** Macros that create ChangeListener[_] and InvalidationListener instances
 *  out of functions and blocks.
 *  Due to the 'Parameter type in structural refinement may not refer to an abstract type defined outside that refinement' restriction, had to do some hacks with AnyRef instead of T.
 */
private[fx] object ObservableValueExtensionMacros
{
  def onChangeFunction[T : c.WeakTypeTag]
      (c: Context)
      (f: c.Expr[T => Unit]): c.Expr[Unit] =
  {
    import c.universe._

    val Apply(_, List(value)) = c.prefix.tree
    val valueExpr = c.Expr[ObservableValue[T]](value)

    reify(
      valueExpr.splice.addListener(
        new ChangeListener[AnyRef]() {
          override def changed(observable: ObservableValue[AnyRef], oldValue: AnyRef, newValue: AnyRef) {
            f.splice(newValue.asInstanceOf[T])
          }
        }.asInstanceOf[ChangeListener[T]]
      )
    )
  }

  def onChangeFunction2[T : c.WeakTypeTag]
      (c: Context)
      (f: c.Expr[(T, T) => Unit]): c.Expr[Unit] =
  {
    import c.universe._

    val Apply(_, List(value)) = c.prefix.tree
    val valueExpr = c.Expr[ObservableValue[T]](value)

    reify(
      valueExpr.splice.addListener(
        new ChangeListener[AnyRef]() {
          override def changed(observable: ObservableValue[AnyRef], oldValue: AnyRef, newValue: AnyRef) {
            f.splice(oldValue.asInstanceOf[T], newValue.asInstanceOf[T])
          }
        }.asInstanceOf[ChangeListener[T]]
      )
    )
  }

  def onChangeBlock[T : c.WeakTypeTag]
      (c: Context)
      (block: c.Expr[Unit]): c.Expr[Unit] =
  {
    import c.universe._

    val Apply(_, List(value)) = c.prefix.tree
    val valueExpr = c.Expr[ObservableValue[T]](value)

    reify(
      valueExpr.splice.addListener(
        new ChangeListener[AnyRef]() {
          override def changed(observable: ObservableValue[AnyRef], oldValue: AnyRef, newValue: AnyRef) {
            block.splice
          }
        }.asInstanceOf[ChangeListener[T]]
      )
    )
  }

  def onInvalidate[T : c.WeakTypeTag]
      (c: Context)
      (block: c.Expr[Unit]): c.Expr[Unit] =
  {
    import c.universe._

    val Apply(_, List(value)) = c.prefix.tree
    val valueExpr = c.Expr[ObservableValue[T]](value)

    reify(
      valueExpr.splice.addListener(
        new InvalidationListener() {
          override def invalidated(observable: Observable) {
            block.splice
          }
        }
      )
    )
  }
}
