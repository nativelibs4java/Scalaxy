package scalaxy.reified.internal

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._
import scala.reflect.runtime.currentMirror
import scala.tools.reflect.ToolBox

import scalaxy.reified.ReifiedValue

object Utils {

  private[reified] def newExpr[A](tree: Tree): Expr[A] = {
    Expr[A](
      currentMirror,
      CurrentMirrorTreeCreator(tree))
  }

  def typeCheck[A](expr: Expr[A], pt: Type = WildcardType): Expr[A] = {
    newExpr[A](typeCheckTree(expr.tree, pt))
  }

  private[reified] val optimisingOptions = "-optimise -Yclosure-elim -Yinline"
  private[reified] val optimisingToolbox = {
    currentMirror.mkToolBox(options = optimisingOptions)
  }

  private[reified] def getModulePath(u: scala.reflect.api.Universe)(moduleSym: u.ModuleSymbol): u.Tree = {
    import u._
    def rec(relements: List[String]): Tree = relements match {
      case name :: Nil =>
        Ident(name: TermName)
      case ("`package`") :: rest =>
        //rec(rest)
        Select(rec(rest), "package": TermName)
      case name :: rest =>
        Select(rec(rest), name: TermName)
    }
    rec(moduleSym.fullName.split("\\.").reverse.toList)
  }

  private[reified] def resolveModulePaths(u: scala.reflect.api.Universe)(root: u.Tree): u.Tree = {
    import u._
    new Transformer {
      override def transform(tree: Tree) = tree match {
        case Ident() if tree.symbol != null && tree.symbol.isModule =>
          getModulePath(u)(tree.symbol.asModule)
        case _ =>
          super.transform(tree)
      }
    }.transform(root)
  }

  private[reified] def typeCheckTree(tree: Tree, pt: Type = WildcardType): Tree = {
    try {
      optimisingToolbox.typeCheck(tree, pt)
    } catch {
      case ex: Throwable =>
        throw new RuntimeException(s"Failed to typeCheck($tree, $pt): $ex", ex)
    }
  }
}
