package scalaxy

import scala.reflect.runtime.universe._

trait MatchAction {
  def pattern: Expr[_]
}

case class Replacement(
  pattern: Expr[Any], replacement: Expr[Any])
extends MatchAction

case class MatchError(
  pattern: Expr[Any],
  message: String)
extends MatchAction

case class MatchWarning(
  pattern: Expr[Any],
  message: String)
extends MatchAction

sealed trait Action[T]

case class ReplaceBy[T](
  replacement: Expr[T])
extends Action[T]

case class Error[T](
  message: String)
extends Action[T]

case class Warning[T](
  message: String)
extends Action[T]

case class ConditionalAction[T](
  pattern: Expr[T],
  when: Seq[String],
  thenMatch: PartialFunction[List[Tree], Action[T]])
extends MatchAction {
  def patternTree = pattern.tree
}

