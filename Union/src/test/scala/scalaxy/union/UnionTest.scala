package scalaxy.union.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror
import scala.tools.reflect.ToolBox

import scala.reflect.runtime.currentMirror

import scalaxy.union._

//@Ignore
class UnionTest {

  lazy val tb = currentMirror.mkToolBox()

  def compile(code: String, expectSuccess: Boolean = true, prefix: String = "import scalaxy.union._\n"): () => Any = {
    val fullCode = prefix + code
    try {
      val res = tb.compile(tb.parse(fullCode))
      if (!expectSuccess) {
        fail("Didn't expect success from compilation of: " + fullCode)
      }
      res
    } catch {
      case ex: Throwable =>
        if (expectSuccess) {
          fail("Didn't expect failure from compilation of: " + fullCode + "\n" + ex)
        }
        null
    }
  }

  @Test
  def testUnionTypeClasses {
    type TC[T] = T <|< (Int | Float)
    implicitly[TC[Int]]

    implicitly[Number[Int]]
    compile("implicitly[Number[Int]]")
    compile("implicitly[Number[Char]]", false)

    val f = "def f[N: Number](n: N) = n \n"
    compile(f + "f(10)")
    compile(f + "f('10')", false)
  }

  @Test
  def testSimpleUnionsDerives {
    implicitly[Int <|< Int]
    implicitly[Int <|< (Int | Float)]

    compile("implicitly[List[_] <|< (Seq[_] | String)]")
    compile("implicitly[Seq[_] <|< List[_]]", false)
    compile("implicitly[String <|< Seq[_]]", false)
  }

  @Test
  def testSimpleUnionsMatches {
    implicitly[Int =|= Int]
    implicitly[Int =|= (Int | Float)]

    compile("implicitly[List[_] =|= (Seq[_] | String)]", false)
    compile("implicitly[Seq[_] =|= List[_]]", false)
    compile("implicitly[Seq[_] =|= Seq[_]]")
  }

  @Test
  def testTraitUnion {
    implicitly[Int <|< (Int | Float)]
    type TC[T] = T <|< (Int | Float)
    implicitly[TC[Int]]

    val d = "import scalaxy.union.test.UnionTest._\n"
    compile(d + """serialize("blah")""")
    compile(d + """serialize(10.0)""")
    compile(d + """serialize(10)""", false)
    compile(d + """serialize(Map("a" -> 10.0))""")
    compile(d + """serialize(Map(10.0 -> "a"))""", false)
    compile(d + """serialize(Array("a", 10.0))""")
    compile(d + """serialize(Array('a', 10.0))""", false)
    compile(d + """serialize(Array(10))""", false)
    compile(d + """serialize(Array())""")
  }
}

object UnionTest {
  //   import scala.language.experimental.macros
  //   @scala.annotation.implicitNotFound(msg = "${T} is not a valid JSON type.")
  //   sealed trait JSONType[T] extends (T <|< (String | Double | Array[JSONType[_]] | Map[String, JSONType[_]]))
  //   object JSONType {
  //     implicit def apply[T]: JSONType[T] = macro prove[T, JSONType[T]]
  //   }
  //   def serialize[T: JSONType](value: T): String = ""
  // import scala.language.experimental.macros
  trait JSONType extends (String | Double | Array[JSONType] | Map[String, JSONType])

  def serialize[T: JSONType#Union](value: T): String = "..."

}
