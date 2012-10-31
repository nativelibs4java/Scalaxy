package scalaxy; package test

import plugin._

import org.junit._
import Assert._

class ForLoopsTest extends BaseTestUtils {

  override def pluginDef = new ScalaxyPluginDefLike {
    override def matchActionHolders = Seq(compilets.ForLoops)
  }

  @Test
  def simpleUntilFilterLoop {
    ensurePluginCompilesSnippetsToSameByteCode(
      """ var t = 0
          for (j <- 0 until 100)//; if i < 10)
            t += 2 * j
      """,
      """
          var t = 0;

          {
            var ii = 0
            while (ii < 100) {
              val i = ii
              t += 2 * i
              ii = ii + 1
            }
          }
      """
    )
  }

  @Test
  def simpleUntilByAscFilterLoop {
    ensurePluginCompilesSnippetsToSameByteCode(
      """ var t = 0
          for (i <- 0 until 100 by 2)
            t += 2 * i
      """,
      """
          var t = 0;
          {
            var ii = 0
            while (ii < 100) {
              val i = ii
              t += 2 * i
              ii = ii + 2
            }
          }
      """
    )
  }
  
  @Test
  def simpleUntilByDescFilterLoop {
    ensurePluginCompilesSnippetsToSameByteCode(
      """ var t = 0
          for (i <- 10 until 0 by -2)
            t += 2 * i
      """,
      """
          var t = 0;
          {
            var ii = 10
            while (ii > 0) {
              val i = ii
              t += 2 * i
              ii = ii + - 2
            }
          }
      """
    )
  }
}
