package scalaxy; package examples

object DSLCompilet extends Compilet
{
  import DSL._
  
  import java.util.regex.Pattern.quote
  
  def replaceQuotePattern(s: String) = replace(
    s.quotePattern,
    quote(s)
  )
}
