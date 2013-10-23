package scalaxy.compilets.test

import org.junit._

class StreamsTest extends BaseTestUtils 
{
  override def compilets = Seq(scalaxy.compilets.Streams)

  @Ignore
  @Test
  def listMapMap {
    ensurePluginCompilesSnippetsToSameByteCode(
      """
          val col: List[Int] = (1 to 100).toList 
          col.map(_.toString).map(_.length)
      """,
      """ 
          val col: List[Int] = (1 to 100).toList 
          col.map(a => {
            val b = a.toString
            val c = b.length
            c
          })
      """
    )
  }
  @Ignore
  @Test
  def listMapMapMap {
    ensurePluginCompilesSnippetsToSameByteCode(
      """
          List(1, 2, 3).map(_.toString).map(_.trim).map(_.length)
      """,
      """ 
          TODO
      """
    )
  }
}
