package scalaxy.json.json4s.jackson.test
import scalaxy.json.json4s.jackson._

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
    val byteValue: Byte = 1
    val shortValue: Short = 1
    val intValue: Int = 1
    val longValue: Long = 1
    val floatValue: Float = 1
    val doubleValue: Double = 1
    val stringValue: String = "1"
    val charValue: Char = '1'

    val someKey = "someKey1"
    val somePair1 = "someKey2" -> 100
    val somePair2 = Some("someKey3" -> 1000)
    val noPair1 = None
    val noPair2: Option[(String, JValue)] = None

    //json.blah(1)
    def p(v: JValue) { println(pretty(v)) }

    assertEquals("""{
  "x" : 10.0,
  "y" : "123",
  "s" : "a\nb\tc",
  "z1" : [ 10000000000, {
    "x" : 10
  } ],
  "z2" : 100.01,
  "byte" : 1.0,
  "short" : 1.0,
  "int" : 1.0,
  "long" : 1.0,
  "float" : 1.0,
  "double" : 1.0,
  "string" : "1",
  "char" : "1",
  "someKey1" : 10,
  "someKey2" : 100.0,
  "someKey3" : 1000.0
}""", pretty(json"""{
  "x": $a,
  y: $b,
  s: "a\nb\tc",
  z1: [10000000000, { x: 10 }],
  z2: 100.01,
  byte: $byteValue,
  short: $shortValue,
  int: $intValue,
  long: $longValue,
  float: $floatValue,
  double: $doubleValue,
  string: $stringValue,
  char: $charValue,
  $noPair1,
  $someKey: 10,
  $somePair1,
  $somePair2,
  $noPair2
}"""))

    assertEquals("""{
  "x" : 10.0,
  "y" : "123"
}""", pretty(json(x = a, y = b)))

    assertEquals("""[ 10.0, "123" ]""", pretty(json"[$a, $b]"))
    assertEquals("""[ 10.0, "123" ]""", pretty(json(a, b)))

    // json"""{,}"""
    // assertEquals(JNothing, parse("{,e}"))
    // json"""{,e}"""
  }

  def eval(src: String) = {
    JSONTest.eval("import scalaxy.json.json4s.jackson._\n" + src)
  }

  def assertEvalException(src: String, msg: String) {
    try {
      eval(src)
      fail("Expected eval error: " + msg)
    } catch { case ex: scala.tools.reflect.ToolBoxError =>
      assertEquals("scala.tools.reflect.ToolBoxError: reflective compilation has failed: \n\n" + msg, ex.toString)
    }
  }
  @Test
  def testErrors {
    assertEvalException(
      """ json"{,}" """,
      "Unexpected character (',' (code 44)): was expecting either valid name character (for unquoted name) or double-quote (for quoted) to start field name")

    assertEvalException(
      """ json"[a,]" """,
      "Unexpected character ('a' (code 97)): expected a valid value (number, String, array, object, 'true', 'false' or 'null')")
  }

  @Test
  def testDeconstructObject {
    {
      val json"{ x: $a, y: $b, s: ${JString(s)} }" =
        """{ y: 10.0, s: "!", x: [1.0, 2.0, 3.0] }"""

      assertEquals(JDouble(10), b)
      assertEquals("!", s)
      assertEquals(JArray(List(JDouble(1), JDouble(2), JDouble(3))), a)
    }

    {
      val x = 10
      val json"{ x: ${JDouble(y)} }" = json"{ x: $x }"
      assertEquals(x, y, 0)
    }

    // {
    //   val json"{ x: ${JDouble(x)}, ... }" = "{ x: 1.0, b: 10 }"
    //   assertEquals(1.0, x, 0)
    // }
  }

  def removeJsonStrings(s: String) = {
    s.replaceAll("""
      "([^\\"]+|\\"|\\\\|\\\w)*"
    """.trim, "")
  }
  @Test
  def dots {
    assertEquals("a  c  e   f", removeJsonStrings("""
      a "a\"b" c "d\\" e "\"" "\"...\"" f
    """).trim)
  }

  @Test
  def testDeconstructArray {
    {
      val json"[ $a, $b, ${JString(s)} ]" =
        """[ 10.0, [1.0, 2.0, 3.0], "!" ]"""

      assertEquals(JDouble(10), a)
      assertEquals("!", s)
      assertEquals(JArray(List(JDouble(1), JDouble(2), JDouble(3))), b)
    }
  }
}

object JSONTest {

  import scala.reflect.runtime.universe._
  import scala.reflect.runtime.currentMirror
  import scala.tools.reflect.ToolBox

  val toolbox = currentMirror.mkToolBox()

  def eval(src: String): Any = {
    toolbox.eval(toolbox.parse(src))
  }
}
