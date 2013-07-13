package plots
package test

import scala.io.Source

import org.junit._
import Assert._

import CSV._

class CSVTest
{
  @Test
  def simpleCSV {
    val s = """
      a;b ;c; d
      1 2 3 4
      10; 20;30; 40
    """
    val res = readCSVFromSource(Source.fromString(s), "test")("b", "d") {
      case Array(b, d) => (b.toInt, d.toInt)
    }
    assertEquals(List((2, 4), (20, 40)), res.toList)
  }
  
  @Test
  def simpleFields {
    val s = """
      % blah beh
      1 2 3 4
      10 20 30 40
    """
    val res = readFieldsFromSource(Source.fromString(s), "test") {
      case Array(a, b, c, d) => (b.toInt, d.toInt)
    }
    assertEquals(List((2, 4), (20, 40)), res.toList)
  }
}
