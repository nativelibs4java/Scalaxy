package scalaxy
//package macros

//import language.experimental.macros

import scala.reflect.runtime._
import scala.reflect.runtime.universe._

object RuntimeMirrorUtils {
  def expr[T](tree: Tree): Expr[T] = {
    Expr[T](
      currentMirror,//.runtimeMirror, 
      new reflect.base.TreeCreator { 
        override def apply[U <: reflect.base.Universe with Singleton](m: reflect.base.MirrorOf[U]) =
          tree.asInstanceOf[U#Tree]
      }
    )
  }
}
import RuntimeMirrorUtils._

trait MatchAction {
  def pattern: Expr[_]
  
  def typeCheck(f: (Tree, Type) => Tree): MatchAction
}

case class Replacement(
  pattern: Expr[Any], replacement: Expr[Any]
) extends MatchAction {
  override def typeCheck(f: (Tree, Type) => Tree) =
    Replacement(
      expr[Any](f(pattern.tree, pattern.staticTpe)),
      expr[Any](f(replacement.tree, replacement.staticTpe))
    )
}

case class MatchError(
  pattern: Expr[Any], 
  message: String
) extends MatchAction {
  override def typeCheck(f: (Tree, Type) => Tree) =
    MatchError(
      expr[Any](f(pattern.tree, pattern.staticTpe)),
      message
    )
}

case class MatchWarning(
  pattern: Expr[Any], 
  message: String
) extends MatchAction {
  override def typeCheck(f: (Tree, Type) => Tree) =
    MatchWarning(
      expr[Any](f(pattern.tree, pattern.staticTpe)),
      message
    )
}

sealed trait Action[T] {
  def typeCheck(f: (Tree, Type) => Tree): Action[T]
}

case class ReplaceBy[T](
  replacement: Expr[T]
) extends Action[T] {
  override def typeCheck(f: (Tree, Type) => Tree) =
    ReplaceBy[T](
      expr[T](f(replacement.tree, replacement.staticTpe))
    )
}

case class Error[T](
  message: String
) extends Action[T] {
  override def typeCheck(f: (Tree, Type) => Tree) =
    this
}

case class Warning[T](
  message: String
) extends Action[T] {
  override def typeCheck(f: (Tree, Type) => Tree) =
    this
}

case class ConditionalAction[T](
  pattern: Expr[T], 
  when: Seq[String], 
  thenMatch: PartialFunction[List[Tree], Action[T]]
) extends MatchAction {
  def patternTree = pattern.tree
  
  override def typeCheck(f: (Tree, Type) => Tree) =
    ConditionalAction[T](
      expr[T](f(pattern.tree, pattern.staticTpe)),
      when,
      new PartialFunction[List[Tree], Action[T]] {
        override def isDefinedAt(list: List[Tree]) =
          thenMatch.isDefinedAt(list)
        override def apply(list: List[Tree]) =
          thenMatch(list).typeCheck(f)
      }
    )
}

