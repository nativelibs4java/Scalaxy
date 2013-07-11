package scalaxy.reified.impl

import scala.reflect.runtime.universe._
import scala.reflect.runtime.currentMirror
import scala.tools.reflect.ToolBox

object Utils {

  private[reified] def newExpr[A](tree: Tree): Expr[A] = {
    Expr[A](
      currentMirror,
      CurrentMirrorTreeCreator(tree))
  }

  def typeCheck[A](expr: Expr[A]): Expr[A] = {
    newExpr[A](typeCheck(expr.tree))
  }

  private[reified] val toolbox = currentMirror.mkToolBox()

  def typeCheck(tree: Tree, pt: Type = WildcardType): Tree = {
    // TODO reuse toolbox if costly to create and if doesn't take too much memory. 
    val ttree = tree.asInstanceOf[toolbox.u.Tree]
    if (ttree.tpe != null && ttree.tpe != NoType)
      tree
    else {
      try {
        toolbox.typeCheck(
          ttree,
          pt.asInstanceOf[toolbox.u.Type])
      } catch {
        case ex: Throwable =>
          throw new RuntimeException(s"Failed to typeCheck($tree, $pt): $ex", ex)
      }
    }.asInstanceOf[Tree]
  }
}
