package scalaxy.json.test
import scalaxy.json._

import org.junit._
import Assert._

class JSONTest {
  @Test
  def simple {

    val a = 10
    val b = "123"

    //json.blah(1)

    json"""{ "x": $a, "y": $b }"""
    json(x = a, y = b)

    // json"[$a, $b]"
    json(a, b)
  }
}
