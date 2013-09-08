package scalaxy.json.test
import scalaxy.json.jackson._

import org.json4s._
import org.json4s.jackson.JsonMethods._
// import org.json4s.native.JsonMethods._

import org.junit._
import Assert._

class JSONTest {
  @Test
  def simple {

    val a = 10
    val b = "123"

    //json.blah(1)

    json"""{ "x": $a, y: $b }"""
    json(x = a, y = b)

    // json"[$a, $b]"
    json(a, b)

    // json"""{,}"""
    // assertEquals(JNothing, parse("{,e}"))
    // json"""{,e}"""
  }
}
