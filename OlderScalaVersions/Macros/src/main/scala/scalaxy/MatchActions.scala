package scalaxy
//package macros

//import language.experimental.macros

import scala.reflect.mirror._

sealed trait Action[T]

trait MatchAction[T] extends Action[T] {
  def pattern: Expr[T]
}

case class ReplaceBy[T](
  replacement: Expr[T]
) extends Action[T]

case class Error[T](
  message: String
) extends Action[T]

case class Warning[T](
  message: String
) extends Action[T]

case class Replacement[T](
  pattern: Expr[T], 
  replacement: Expr[T]
) extends MatchAction[T]

case class MatchError[T](
  pattern: Expr[T], 
  message: String
) extends MatchAction[T]

case class MatchWarning[T](
  pattern: Expr[T], 
  message: String
) extends MatchAction[T]

case class ConditionalAction[T](
  pattern: Expr[T], 
  when: Seq[String], 
  thenMatch: PartialFunction[List[Tree], Action[T]]
) extends MatchAction[T]

