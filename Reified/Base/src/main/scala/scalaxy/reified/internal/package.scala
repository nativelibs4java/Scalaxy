package scalaxy.reified

import scala.language.experimental.macros

import scala.reflect._
import scala.reflect.macros.Context
import scala.reflect.runtime.universe

import scalaxy.reified.internal.Utils._
import scalaxy.generic.Generic

package object internal {

  private[reified] lazy val verbose =
    System.getProperty("scalaxy.reified.verbose") == "true" ||
      System.getenv("SCALAXY_REIFIED_VERBOSE") == "1"

  private[reified] val syntheticVariableNamePrefix = "scalaxy$reified$"

  private def runtimeExpr[A](c: Context)(tree: c.universe.Tree): c.Expr[universe.Expr[A]] = {
    import c.universe._
    // new Traverser {
    //   override def traverse(tree: Tree) {
    //     if (tree.symbol != null) {
    //       if (tree.symbol.isFreeTerm) {
    //         println(s"tree $tree: ${tree.symbol}")
    //         println("\tvalue = " + tree.symbol.asFreeTerm.value + ": " + tree.symbol.typeSignature)
    //       }
    //     }
    //     super.traverse(tree)
    //   }
    // } traverse tree

    c.Expr[universe.Expr[A]](
      c.reifyTree(
        c.universe.treeBuild.mkRuntimeUniverseRef,
        c.universe.EmptyTree,
        tree
      )
    )
  }

  private[reified] def reifyMacro[A: universe.TypeTag](v: A): ReifiedValue[A] = macro reifyImpl[A]
  private[reified] def reifyWithDifferentRuntimeValue[A: universe.TypeTag](v: A, runtimeValue: A): ReifiedValue[A] = macro reifyWithDifferentRuntimeValueImpl[A]

  def reifyImpl[A: c.WeakTypeTag](c: Context)(v: c.Expr[A])(tt: c.Expr[universe.TypeTag[A]]): c.Expr[ReifiedValue[A]] = {
    import c.universe._

    val expr = runtimeExpr[A](c)(c.typeCheck(v.tree))
    val res = reify({
      implicit val valueTag: universe.TypeTag[A] = tt.splice
      new ReifiedValue[A](
        v.splice,
        Utils.typeCheck(expr.splice, valueTag.tpe))
    })
    res
  }

  def reifyWithDifferentRuntimeValueImpl[A: c.WeakTypeTag](c: Context)(v: c.Expr[A], runtimeValue: c.Expr[A])(tt: c.Expr[universe.TypeTag[A]]): c.Expr[ReifiedValue[A]] = {

    import c.universe._

    val expr = runtimeExpr[A](c)(c.typeCheck(v.tree))
    reify({
      implicit val valueTag = tt.splice
      new ReifiedValue[A](
        runtimeValue.splice,
        Utils.typeCheck(expr.splice, valueTag.tpe))
    })
  }
}
