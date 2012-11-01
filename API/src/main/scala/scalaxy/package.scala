import language.experimental.macros
import scala.reflect.runtime.universe._

package object scalaxy
{
  def fail(message: String)(pattern: Any): MatchError =
    macro scalaxy.macros.fail

  def warn(message: String)(pattern: Any): MatchWarning =
    macro scalaxy.macros.warn

  def replace[T](pattern: T, replacement: T): Replacement =
    macro scalaxy.macros.replace[T]

  def when[T](pattern: T)(idents: Any*)(thenMatch: PartialFunction[List[Tree], Action[T]]) : ConditionalAction[T] =
    macro scalaxy.macros.when[T]

  def error[T](message: String): Action[T] =
    Error[T](message)

  def warning[T](message: String): Action[T] =
    Warning[T](message)

  def replacement[T](replacement: T): ReplaceBy[T] =
    macro scalaxy.macros.replacement[T]
}