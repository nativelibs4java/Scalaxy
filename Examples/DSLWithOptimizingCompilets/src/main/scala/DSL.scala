package scalaxy.compilets
package examples

object DSL extends Compilet
{
  import java.util.regex.Pattern.quote
  
  implicit def stringExtensions(s: String) = new {
    def quotePattern = quote(s)
  }
}
