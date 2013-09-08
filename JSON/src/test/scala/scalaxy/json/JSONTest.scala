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
    val n = "someKey"

    //json.blah(1)
    def p(v: JValue) { println(pretty(v)) }

    p(json"""{
      "x": $a,
      y: $b,
      z1: [10000000000, { x: 10 }],
      z2: 100.01,
      $n: 10
    }""")
    p(json(x = a, y = b))

    p(json"[$a, $b]")
    p(json(a, b))

    // json"""{,}"""
    // assertEquals(JNothing, parse("{,e}"))
    // json"""{,e}"""
  }
}
