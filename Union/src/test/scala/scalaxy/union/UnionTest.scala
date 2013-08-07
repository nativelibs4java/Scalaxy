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

  def compile(
    code: String,
    expectSuccess: Boolean = true,
    prefix: String = """
        import scalaxy.union._
        import scalaxy.union.test.UnionTest._
      """): () => Any = {
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

    compile("""serialize("blah")""")
    compile("""serialize(10.0)""")
    compile("""serialize(10)""", false)
    compile("""serialize(Map[String, JSONValue]("a" -> 10.0.as[JSONValue]))""")
    compile("""serialize(Map[Double, JSONValue](10.0 -> "a".as[JSONValue]))""", false)
    // compile("""serialize(Array("a", 10.0))""")
    // compile("""serialize(Array('a', 10.0))""", false)
    compile("""serialize(Array[JSONValue]())""")
    compile("""serialize(Array[JSONValue](10))""", false)
  }

  import UnionTest._

  @Test
  def testCast {
    assertEquals(10.0, 10.0.as[JSONValue].value)
    assertEquals("1", "1".as[JSONValue].value)

    compile("""'1'.as[JSONValue]""", false)
    compile("""10.as[JSONValue]""", false)
    compile("""Map("a" -> 10.0).as[JSONValue]""")
    compile("""Map("a" -> 10).as[JSONValue]""", false)
  }
}

object UnionTest {
  import scala.language.experimental.macros
  import scala.language.implicitConversions

  // @scala.annotation.implicitNotFound(msg = "${T} is not a valid JSON type.") //   sealed trait JSONType[T] extends (T <|< (String | Double | Array[JSONType[_]] | Map[String, JSONType[_]]))
  //   object JSONType {
  //     implicit def apply[T]: JSONType[T] = macro prove[T, JSONType[T]]
  //   }
  //   def serialize[T: JSONType](value: T): String = ""
  // import scala.language.experimental.macros
  abstract class JSONValue extends (String | Double | Array[JSONValue] | Map[String, JSONValue])
  object JSONValue {
    implicit def apply[A](value: A): JSONValue = macro wrap[A, JSONValue]
  }

  trait JSONScalar extends (String | Double)

  def serialize[T: JSONValue#Union](value: T): String = "..."

}
