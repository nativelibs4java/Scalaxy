package scalaxy

import scala.reflect.mirror._

package object macros
{ 
  /*
  def fail[T](message: String)(pattern: T): MatchAction[T] = 
    macro MacroImpls.fail[T]
  
  def warn[T](message: String)(pattern: T): MatchAction[T] = 
    macro MacroImpls.warn[T]
  
  def replace[T](pattern: T, replacement: T): Replacement[T] = 
    macro MacroImpls.replace[T]
  
  def when[T](pattern: T)(idents: Any*)(thenMatch: PartialFunction[List[Tree], Action[T]]) : ConditionalAction[T] = 
    macro MacroImpls.when[T]
  
  def error[T](message: String): Action[T] =
    Error[T](message)
  
  def warning[T](message: String): Action[T] =
    Warning[T](message)
    
  def replacement[T](replacement: T): ReplaceBy[T] = 
    macro MacroImpls.replacement[T]
  */
}