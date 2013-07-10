package scalaxy.reified

import scala.language.experimental.macros

import scala.reflect._
import scala.reflect.macros.Context
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._
import scala.reflect.runtime.currentMirror
import scala.tools.reflect.ToolBox

import scalaxy.reified.impl.Capture

class ReifiedValue[A](
    val value: A,
    private[reified] val rawExpr: Expr[A],
    val captures: Seq[AnyRef]) {

  import ReifiedValue._
  val expr: Expr[A] = {
    val transformer = new Transformer {
      override def transform(tree: Tree): Tree = {
        tree match {
          case Capture(captureIndex) =>
            val capturedValue = captures(captureIndex)
            capturedValue match {
              case v: Integer =>
                Literal(Constant(v))
              case r: ReifiedValue[_] =>
                r.expr.tree.duplicate
              case _ =>
                sys.error(s"This type of captured value is not supported: $capturedValue")
            }
          case _ =>
            super.transform(tree)
        }
      }
    }
    
    //val toolBox = runtimeMirror(c.libraryClassLoader).mkToolBox()
    //lazy val toolbox = currentMirror.mkToolBox()
  
  
    val rawChecked = typeCheck(rawExpr.tree)
    Expr[A](
      currentMirror,
      CurrentMirrorTreeCreator(transformer.transform(rawChecked)))
  }
}

object ReifiedValue {
  import scala.reflect.api._
  
  lazy val toolbox = currentMirror.mkToolBox()
  
  def typeCheck(tree: Tree, pt: Type = WildcardType): Tree = {
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
  
  case class CurrentMirrorTreeCreator(tree: Tree) extends TreeCreator {
    def apply[U <: Universe with Singleton](m: scala.reflect.api.Mirror[U]): U # Tree = {
      if (m eq currentMirror) {
        tree.asInstanceOf[U # Tree]
      } else {
        throw new IllegalArgumentException(s"Expr defined in current mirror cannot be migrated to other mirrors.")
      }
    }
  }
}
