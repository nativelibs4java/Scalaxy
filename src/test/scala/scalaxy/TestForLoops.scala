package scalaxy; package test

import plugin._

import org.junit._
import Assert._

//@Ignore
class ForLoopsTest extends BaseTestUtils {

  override def pluginDef = new ScalaxyPluginDefLike {
    override def matchActionHolders = Seq(rewrites.ForLoops)
  }
  
  @Test
  def simpleUntilFilterLoop {
    ensurePluginCompilesSnippetsToSameByteCode(
      """ var t = 0
          for (i <- 0 until 100)//; if i < 10)
            t += 2 * i
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
    
}
