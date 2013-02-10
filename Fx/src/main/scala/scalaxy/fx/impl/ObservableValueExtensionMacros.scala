package scalaxy.fx
package impl

import javafx.beans._
import javafx.beans.binding._
import javafx.beans.property._
import javafx.beans.value._
import javafx.event._

import scala.language.experimental.macros
import scala.reflect.macros.Context

import scalaxy.fx.runtime.ScalaChangeListener

/** Macros that create ChangeListener[_] and InvalidationListener instances
 *  out of functions and blocks.
 *
 *  Note that we need a single runtime class to implement ChangeListener[T] due to a bug in Scala macros:
 *  <code>
 *    'Error: unexpected: bound type that doesn't have a tpe'
 *  </code>
 *
 *  Otherwise we could use the following below:
 *  <pre><code>
 *    val valueExpr = c.Expr[ObservableValue[T]](value)
 *    reify(
 *      valueExpr.splice.addListener(
 *        new ChangeListener[T]() {
 *          override def changed(
 *              observable: ObservableValue[_ <: T],
 *              oldValue: T,
 *              newValue: T)
 *          {
 *            f.splice(oldValue, newValue)
 *          }
 *        }
 *      )
 *    )
 *  </code></pre>
 */
private[fx] object ObservableValueExtensionMacros
{
  private def getAddListenerMethod(c: Context)(tpe: c.universe.Type): c.universe.Symbol = {
    import c.universe._

    tpe.member(newTermName("addListener"))
      .filter(s => s.isMethod && (s.asMethod.paramss.flatten match {
        case Seq(param) if param.typeSignature <:< typeOf[ChangeListener[_]] =>
          true
        case _ =>
          false
      }))
  }

  def onChangeFunction[T : c.WeakTypeTag]
      (c: Context)
      (f: c.Expr[T => Unit]): c.Expr[Unit] =
  {
    import c.universe._

    val Apply(_, List(value)) = c.prefix.tree
    c.Expr[Unit](
      Apply(
        Select(value, getAddListenerMethod(c)(value.tpe)),
        List(
          reify(
            new ScalaChangeListener[T] {
              override def changed(/*observable: ObservableValue[_ <: T], */oldValue: T, newValue: T) {
                f.splice(newValue)
              }
            }
          ).tree
        )
      )
    )
  }

  def onChangeFunction2[T : c.WeakTypeTag]
      (c: Context)
      (f: c.Expr[(T, T) => Unit]): c.Expr[Unit] =
  {
    import c.universe._

    val Apply(_, List(value)) = c.prefix.tree
    c.Expr[Unit](
      Apply(
        Select(value, getAddListenerMethod(c)(value.tpe)),
        List(
          reify(
            new ScalaChangeListener[T] {
              override def changed(/*observable: ObservableValue[_ <: T], */oldValue: T, newValue: T) {
                f.splice(oldValue, newValue)
              }
            }
          ).tree
        )
      )
    )
  }

  def onChangeBlock[T : c.WeakTypeTag]
      (c: Context)
      (block: c.Expr[Unit]): c.Expr[Unit] =
  {
    import c.universe._

    val Apply(_, List(value)) = c.prefix.tree
    //val TypeRef(_, _, List(valueTpe)) = value.tpe
    println(s"value.tpe ${value.tpe}, T ${weakTypeTag[T].tpe}")
    c.Expr[Unit](
      Apply(
        Select(value, getAddListenerMethod(c)(value.tpe)),
        List(
          reify(
            new ScalaChangeListener[T] {
              override def changed(/*observable: ObservableValue[_ <: T], */oldValue: T, newValue: T) {
                block.splice
              }
            }
          ).tree
        )
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
