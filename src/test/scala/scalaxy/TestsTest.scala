package scalaxy; package test

import plugin._

import org.junit._
import Assert._

class TestsTest extends BaseTestUtils {

  override def pluginDef = new ScalaxyPluginDefLike {
    override def matchActionHolders = Seq(rewrites.Test)
  }
  
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
    } catch { case _ => }
  }
  
  @Test
  def testReplacedConstant {
    ensurePluginCompilesSnippetsToSameByteCode(
      """ 
        println(666)
      """,
      """ 
        println(667)
      """
    )
  }
}
