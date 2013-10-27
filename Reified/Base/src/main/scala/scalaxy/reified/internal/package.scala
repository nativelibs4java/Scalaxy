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

  private def runtimeExpr[A: c.WeakTypeTag](c: Context)(tree: c.universe.Tree): c.Expr[universe.Expr[A]] = {
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

  private[reified] def reifiedMacro[A: universe.TypeTag](v: A): Reified[A] = macro reifiedImpl[A]
  private[reified] def reifiedWithDifferentRuntimeValue[A: universe.TypeTag](v: A, runtimeValue: A): Reified[A] = macro reifyWithDifferentRuntimeValueImpl[A]

  def reifiedImpl[A: c.WeakTypeTag](c: Context)(v: c.Expr[A])(tt: c.Expr[universe.TypeTag[A]]): c.Expr[Reified[A]] = {
    import c.universe._

    val expr = runtimeExpr[A](c)(c.typeCheck(v.tree))
    // val expr = runtimeExpr[A](c)(v.tree.duplicate)
    // val expr = runtimeExpr[A](c)(v.tree)
    val res = reify({
      implicit val valueTag: universe.TypeTag[A] = tt.splice
      new Reified[A](
        v.splice,
        // Utils.typeCheck(expr.splice, valueTag.tpe))
        expr.splice)
    })
    res
  }

  def hasReifiedValueToReifiedValueImpl[A: c.WeakTypeTag](c: Context)(r: c.Expr[HasReified[A]])(tt: c.Expr[universe.TypeTag[A]]): c.Expr[Reified[A]] = {
    import c.universe._

    c.Expr[Reified[A]](
      Select(r.tree, "reifiedValue": TermName))
  }

  def hasReifiedValueToValueImpl[A: c.WeakTypeTag](c: Context)(r: c.Expr[HasReified[A]])(tt: c.Expr[universe.TypeTag[A]]): c.Expr[A] = {
    import c.universe._

    c.Expr[A](
      Select(Select(r.tree, "reifiedValue": TermName), "value": TermName))
  }

  def reifyWithDifferentRuntimeValueImpl[A: c.WeakTypeTag](c: Context)(v: c.Expr[A], runtimeValue: c.Expr[A])(tt: c.Expr[universe.TypeTag[A]]): c.Expr[Reified[A]] = {

    import c.universe._

    val expr = runtimeExpr[A](c)(c.typeCheck(v.tree))
    reify({
      implicit val valueTag = tt.splice
      new Reified[A](
        runtimeValue.splice,
        Utils.typeCheck(expr.splice, valueTag.tpe))
    })
  }
}
