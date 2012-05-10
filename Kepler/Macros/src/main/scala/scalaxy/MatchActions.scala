package scalaxy
//package macros

//import language.experimental.macros

import scala.reflect.mirror._

trait MatchAction {
  def pattern: Expr[_]
  
  def typeCheck(f: (Tree, Type) => Tree): MatchAction
}

case class Replacement(
  pattern: Expr[Any], replacement: Expr[Any]
) extends MatchAction {
  override def typeCheck(f: (Tree, Type) => Tree) =
    Replacement(
      Expr[Any](f(pattern.tree, pattern.tpe)),
      Expr[Any](f(replacement.tree, replacement.tpe))
    )
}

case class MatchError(
  pattern: Expr[Any], 
  message: String
) extends MatchAction {
  override def typeCheck(f: (Tree, Type) => Tree) =
    MatchError(
      Expr[Any](f(pattern.tree, pattern.tpe)),
      message
    )
}

case class MatchWarning(
  pattern: Expr[Any], 
  message: String
) extends MatchAction {
  override def typeCheck(f: (Tree, Type) => Tree) =
    MatchWarning(
      Expr[Any](f(pattern.tree, pattern.tpe)),
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
      Expr[T](f(replacement.tree, replacement.tpe))
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
      Expr[T](f(pattern.tree, pattern.tpe)),
      when,
      new PartialFunction[List[Tree], Action[T]] {
        override def isDefinedAt(list: List[Tree]) =
          thenMatch.isDefinedAt(list)
        override def apply(list: List[Tree]) =
          thenMatch(list).typeCheck(f)
      }
    )
}

