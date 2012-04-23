package scalaxy

import scala.reflect._

sealed trait Action[T]
trait MatchAction[T] extends Action[T] {
  def pattern: mirror.Tree
}

case class ReplaceBy[T](replacement: mirror.Tree) extends Action[T]
case class Error[T](message: String) extends Action[T]
case class Warning[T](message: String) extends Action[T]

case class Replacement[T](
  pattern: mirror.Tree, 
  replacement: mirror.Tree
) extends MatchAction[T]
case class MatchError[T](pattern: mirror.Tree, message: String) extends MatchAction[T]
case class MatchWarning[T](pattern: mirror.Tree, message: String) extends MatchAction[T]

case class ConditionalAction[T](
  pattern: mirror.Tree, 
  when: Seq[String], 
  then: PartialFunction[Seq[mirror.Tree], Action[T]]
) extends MatchAction[T]

package object macros {
  
  def macro fail[T](message: String)(pattern: T): MatchAction[T] = {
    New(
      Select(Ident(newTermName("scalaxy")), newTypeName("MatchError")), 
      List(List(reify(pattern), message))
    )
  }
  
  def macro warn[T](message: String)(pattern: T): MatchAction[T] = {
    New(
      Select(Ident(newTermName("scalaxy")), newTypeName("MatchWarning")), 
      List(List(reify(pattern), message))
    )
  }
  
  def macro replace[T](pattern: T, replacement: T): Replacement[T] = {
    New(
      Select(Ident(newTermName("scalaxy")), newTypeName("Replacement")), 
      List(List(reify(pattern), reify(replacement)))
    )
  }
  
  def macro when[T](pattern: T)(identifiers: Any*)(then: PartialFunction[Seq[mirror.Tree], Action[T]])
  : ConditionalAction[T] = 
  {
    val scalaCollection = 
      Select(Ident(newTermName("scala")), newTermName("collection"))
      
    New(
      Select(Ident(newTermName("scalaxy")), newTypeName("ConditionalAction")), 
      List(List(
        reify(pattern), 
        Apply(
          Select(Select(scalaCollection, newTermName("Seq")), newTermName("apply")),
          identifiers.toList.map { case Ident(n) => Literal(Constant(n.toString)) }
        ),
        then
      ))
    )
  }
  
  def error[T](message: String): Action[T] =
    Error[T](message)
  
  def warning[T](message: String): Action[T] =
    Warning[T](message)
    
  def macro replacement[T](replacement: T): ReplaceBy[T] = {
    New(
      Select(Ident(newTermName("scalaxy")), newTypeName("ReplaceBy")), 
      List(List(reify(replacement)))
    )
  }
}