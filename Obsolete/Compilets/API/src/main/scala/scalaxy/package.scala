package scalaxy

import scala.reflect.runtime.universe._
import scala.language.experimental.macros

package object compilets
{
  def fail(message: String)(pattern: Any): MatchError =
    macro impl.fail

  def warn(message: String)(pattern: Any): MatchWarning =
    macro impl.warn

  def replace[T](pattern: T, replacement: T): Replacement =
    macro impl.replace[T]

  def when[T](pattern: T)(idents: Any*)(thenMatch: PartialFunction[List[Tree], Action[T]]) : ConditionalAction[T] =
    macro impl.when[T]

  def error[T](message: String): Action[T] =
    Error[T](message)

  def warning[T](message: String): Action[T] =
    Warning[T](message)

  def replacement[T](replacement: T): ReplaceBy[T] =
    macro impl.replacement[T]
}