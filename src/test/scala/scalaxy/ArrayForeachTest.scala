package scalaxy; package test

import plugin._

import org.junit._
import Assert._

class ArrayForeachTest extends BaseTestUtils {

  override def pluginDef = new ScalaxyPluginDefLike {
    override def matchActionHolders = Seq(compilets.ArrayLoops)
  }

  @Test
  def intArrayForeach {
    ensurePluginCompilesSnippetsToSameByteCode(
      """ var t = 0
          val array = Array(1, 2, 3, 4)
          for (v <- array)
            t += 2 * v
      """,
      """
          var t = 0;
          val array = Array(1, 2, 3, 4)
          
          {
            var i = 0
            val n = array.length
            while (i < n) {
              val x = array(i)
              t += 2 * x
              i += 1
            }
          }
      """
    )
  }
}
