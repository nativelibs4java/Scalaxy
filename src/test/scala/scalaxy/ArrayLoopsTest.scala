package scalaxy.test

import org.junit._

class ArrayForeachTest extends BaseTestUtils 
{
  override def compilets = Seq(scalaxy.compilets.ArrayLoops)

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
            var i = 0; val n = array.length
            while (i < n) {
              val x = array(i)
              t += 2 * x
              i += 1
            }
          }
      """
    )
  }
  @Test
  def intArrayMap {
    ensurePluginCompilesSnippetsToSameByteCode(
      """ var t = 0
          val array = Array(1, 2, 3, 4)
          for (v <- array) yield 2 * v
      """,
      """
          var t = 0;
          val array = Array(1, 2, 3, 4)
          
          {
            var i = 0
            val n = array.length
            val out = scala.collection.mutable.ArrayBuilder.make[Int]()
            while (i < n) {
              val x = array(i)
              out += 2 * x
              i += 1
            }
            out.result()
          }
      """
    )
  }
}
