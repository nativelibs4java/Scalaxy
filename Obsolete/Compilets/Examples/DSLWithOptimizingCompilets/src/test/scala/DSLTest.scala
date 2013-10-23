package scalaxy.test

import org.junit._
import org.junit.Assert._

class DSLTest extends BaseTestUtils
{
  override def compilets = Seq(scalaxy.examples.DSLCompilet)

  override def commonImports = """
    import java.util.regex.Pattern.quote
  """
  
  @Test
  def testReplaceQuotePattern {
    ensurePluginCompilesSnippetsToSameByteCode(
      """
        "abc".quotePattern
      """,
      """
        quote("abc")
      """
    )
  }
}
