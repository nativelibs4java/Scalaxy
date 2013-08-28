
package scalaxy.js.ast

import JS._

import org.junit._
import Assert._

class JSTest {
  val pos = SourcePos("", 0, 0)
  
  @Test
  def simpleFunction {
    assertEquals(
      "function(a) {\n  window.console['log']('foo\\nbar\\'yo');\n}",
      prettyPrint(
        Function(
          None,
          List(
            Ident("a", pos)),
          Block(
            List(
              Apply(
                Select(
                  Select(
                    Ident("window", pos),
                    Ident("console", pos),
                    pos),
                  Literal("log", pos),
                  pos),
                List(
                  Literal(
                    "foo\nbar'yo",
                    pos)),
                pos)),
            pos),
          pos)).value
    )
  }

  @Test
  def simpleJSON {
    assertEquals(
      "{\n  'a': [1, 2],\n  'x': 1,\n  'y': 'blah'\n}",
      prettyPrint(
        JSONObject(
          Map(
            "a" -> JSONArray(List(Literal(1, pos), Literal(2, pos)), pos),
            "x" -> Literal(1, pos),
            "y" -> Literal("blah", pos)),
          pos)).value)
  }

  // @Test
  // def advancedJSON {
  //   assertEquals(
  //     "(function() {\n  var obj = {};\n  var pair;\n  pair = somePair;\n  obj[pair[0]] = pair[1];\n  return obj;\n})()",
  //     prettyPrint(
  //       JSONObject(
  //         Map(
  //           Ident("somePair"),
  //           "x" -> Literal(1, pos),
  //           "y" -> Literal("blah", pos)),
  //         pos)).value)
  // }
}
