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

  def typeCheck[A](expr: Expr[A]): Expr[A] = {
    newExpr[A](typeCheck(expr.tree))
  }

  private[reified] val optimisingToolbox = currentMirror.mkToolBox(options = "-optimise")

  private[reified] def getModulePath(u: scala.reflect.api.Universe)(moduleSym: u.ModuleSymbol): u.Tree = {
    import u._
    val elements = moduleSym.fullName.split("\\.").toList
    def rec(root: Tree, sub: List[String]): Tree = sub match {
      case Nil => root
      case name :: rest => rec(Select(root, name: TermName), rest)
    }
    rec(Ident(elements.head: TermName), elements.tail)
  }

  private[reified] def resolveModulePaths(u: scala.reflect.api.Universe)(root: u.Tree): u.Tree = {
    import u._
    new Transformer {
      override def transform(tree: Tree) = tree match {
        case Ident() if tree.symbol != null && tree.symbol.isModule =>
          //println("REPLACING " + tree + " BY MODULE PATH")
          getModulePath(u)(tree.symbol.asModule)
        case _ =>
          super.transform(tree)
      }
    }.transform(root)
  }

  private[reified] def typeCheck(tree: Tree, pt: Type = WildcardType): Tree = {
    val ttree = tree.asInstanceOf[optimisingToolbox.u.Tree]
    if (ttree.tpe != null && ttree.tpe != NoType)
      tree
    else {
      try {
        optimisingToolbox.typeCheck(
          ttree,
          pt.asInstanceOf[optimisingToolbox.u.Type])
      } catch {
        case ex: Throwable =>
          throw new RuntimeException(s"Failed to typeCheck($tree, $pt): $ex", ex)
      }
    }.asInstanceOf[Tree]
  }
}
