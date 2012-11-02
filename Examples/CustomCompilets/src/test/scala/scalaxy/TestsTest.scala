package scalaxy.test

import org.junit._
import org.junit.Assert._

class ConstantReplacementsTest extends BaseTestUtils
{
  override def compilets = Seq(scalaxy.compilets.ConstantReplacements)

  @Test
  def testDummySame {
    ensurePluginCompilesSnippetsToSameByteCode(
      """
        println(667)
      """,
      """
        println(667)
      """,
      allowSameResult = true
    )
  }

  @Test
  def testNotSame {
    try {
      ensurePluginCompilesSnippetsToSameByteCode(
        """
          println(667)
        """,
        """
          println(668)
        """,
        printDifferences = false
      )
      assertTrue(false)
    } catch { case _: Throwable => }
  }

  @Test
  def testReplacedConstant {
    ensurePluginCompilesSnippetsToSameByteCode(
      """
        println(666)
        //println(888)
      """,
      """
        println(667)
        //println(999)
      """
    )
  }

  /*
  @Test
  def testVarargs {
    ensurePluginCompilesSnippetsToSameByteCode(
      """
        println(String.format("i = %d, j = %d", 1, 2))
      """,
      """
        System.out.printf("i = %d, j = %d" + "\n", 1, 2)
      """
    )
  }
  */
}
