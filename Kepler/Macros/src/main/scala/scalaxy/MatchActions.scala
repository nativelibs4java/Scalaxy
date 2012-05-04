package scalaxy
//package macros

//import language.experimental.macros

import scala.reflect.mirror._

trait MatchAction {
  def patternTree: Tree//Expr[_]
}

object Utils {
  lazy val tb = mkToolBox() 
  def typed[T](x: Expr[T]) = {
    //Expr[T](
    tb.typeCheck(x.tree, x.tpe)
    //)
  }
}

case class Replacement(//[T](
  //pattern: Tree, replacement: Tree
  pattern: Expr[Any], replacement: Expr[Any]
) extends MatchAction {
  def patternTree = pattern.tree ; def replacementTree = replacement.tree
  //def patternTree = pattern ; def replacementTree = replacement
}

case class MatchError(
  pattern: Expr[Any], 
  message: String
) extends MatchAction {
  def patternTree = pattern.tree
}

case class MatchWarning(
  pattern: Expr[Any], 
  message: String
) extends MatchAction {
  def patternTree = pattern.tree
}

sealed trait Action[T]

case class ReplaceBy[T](
  replacement: Expr[T]
) extends Action[T]

case class Error[T](
  message: String
) extends Action[T]

case class Warning[T](
  message: String
) extends Action[T]

case class ConditionalAction[T](
  pattern: Expr[T], 
  when: Seq[String], 
  thenMatch: PartialFunction[List[Tree], Action[T]]
) extends MatchAction {
  def patternTree = pattern.tree
}

