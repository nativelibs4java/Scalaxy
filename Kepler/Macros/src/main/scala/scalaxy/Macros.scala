package scalaxy

import scala.reflect.runtime._
import scala.reflect.runtime.universe._

package object macros
{ 
  def fail(message: String)(pattern: Any): MatchError =
    macro MacroImpls.fail
  
  def warn(message: String)(pattern: Any): MatchWarning =
    macro MacroImpls.warn
  
  def replace[T](pattern: T, replacement: T): Replacement = 
    macro MacroImpls.replace[T]
  
  def when[T](pattern: T)(idents: Any*)(thenMatch: PartialFunction[List[Tree], Action[T]]) : ConditionalAction[T] = 
    macro MacroImpls.when[T]
  
  def error[T](message: String): Action[T] =
    Error[T](message)
  
  def warning[T](message: String): Action[T] =
    Warning[T](message)
    
  def replacement[T](replacement: T): ReplaceBy[T] = 
    macro MacroImpls.replacement[T]
}