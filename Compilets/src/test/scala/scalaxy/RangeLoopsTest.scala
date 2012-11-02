package scalaxy.test

import org.junit._

class RangeForeachTest extends BaseTestUtils
{
  override def compilets = Seq(scalaxy.compilets.RangeLoops)

  @Test
  def until {
    ensurePluginCompilesSnippetsToSameByteCode(
      """ var t = 0
          for (j <- 0 until 100)
            t += 2 * j
      """,
      """ var t = 0;
          {
            var ii = 0; val e = 100
            while (ii < e) {
              val i = ii;  t += 2 * i; ii = ii + 1
            }
          }
      """
    )
  }
  @Test
  def to {
    ensurePluginCompilesSnippetsToSameByteCode(
      """ var t = 0
          for (j <- 0 to 100)
            t += 2 * j
      """,
      """ var t = 0;
          {
            var ii = 0; val e = 100
            while (ii <= e) {
              val i = ii;  t += 2 * i; ii = ii + 1
            }
          }
      """
    )
  }

  @Test
  def untilByAsc {
    ensurePluginCompilesSnippetsToSameByteCode(
      """ var t = 0
          for (i <- 0 until 100 by 2)
            t += 2 * i
      """,
      """ var t = 0;
          {
            var ii = 0; val e = 100;
            while (ii < e) {
              val i = ii; t += 2 * i; ii = ii + 2
            }
          }
      """
    )
  }

  @Test
  def toByAsc {
    ensurePluginCompilesSnippetsToSameByteCode(
      """ var t = 0
          for (i <- 0 to 100 by 2)
            t += 2 * i
      """,
      """ var t = 0;
          {
            var ii = 0; val e = 100;
            while (ii <= e) {
              val i = ii; t += 2 * i; ii = ii + 2
            }
          }
      """
    )
  }
  
  @Test
  def untilByDesc {
    ensurePluginCompilesSnippetsToSameByteCode(
      """ var t = 0
          for (i <- 10 until 0 by -2)
            t += 2 * i
      """,
      """ var t = 0;
          {
            var ii = 10; val e = 0
            while (ii > e) {
              val i = ii; t += 2 * i; ii = ii + - 2
            }
          }
      """
    )
  }
  @Test
  def toByDesc {
    ensurePluginCompilesSnippetsToSameByteCode(
      """ var t = 0
          for (i <- 10 to 0 by -2)
            t += 2 * i
      """,
      """ var t = 0;
          {
            var ii = 10; val e = 0
            while (ii >= e) {
              val i = ii; t += 2 * i; ii = ii + - 2
            }
          }
      """
    )
  }
}
