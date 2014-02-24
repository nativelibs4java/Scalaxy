package scalaxy.privacy.test

import scala.language.existentials

import scalaxy.privacy.PrivacyCompiler
import scala.tools.nsc.reporters.{ StoreReporter, Reporter }
import org.junit._
import org.junit.Assert._

class PrivacyTest {

  def compile(src: String): List[String] = {
    import java.io._
    val f = File.createTempFile("test", ".scala")
    try {
      val out = new PrintWriter(f)
      out.print(src)
      out.close()

      val reporter: StoreReporter =
        PrivacyCompiler.compile(
          Array(f.toString, "-d", f.getParent),
          settings => new StoreReporter)
      println(reporter.infos.mkString("\n"))

      reporter.infos.toList.map(_.msg)
    } finally {
      f.delete()
    }
  }

  @Test
  def allPublic {
    assertEquals(
      Nil,
      compile("""
        @public object Foo {
          @public class Bar {
            @public val v = 10
            @public def f: Int = v
          }
        }
        object Test {
          val b = new Foo.Bar
          println(b.v + b.f)
        }
      """)
    )
  }
  @Test
  def noPrivacy {
    assertEquals(
      Nil,
      compile("""
        @noprivacy object Foo {
          class Bar {
            val v: Int = 10
            def f: Int = v
          }
        }
        object Test {
          val b = new Foo.Bar;
          println(b.v + b.f)
        }
      """)
    )
  }

  @Test
  def simpleErrors {
    assertEquals(
      List(
        "value v is not a member of Foo.Bar",
        "value f is not a member of Foo.Bar",
        "type Baz is not a member of object Foo"
      ),
      compile("""
        @public object Foo {
          @public class Bar {
            val v = 10
            def f = v
          }
          class Baz
        }
        object Test {
          val b: Foo.Bar = new Foo.Bar;
          println(b.v)
          println(b.f)

          new Foo.Baz;
        }
      """)
    )
  }
  // TODO: embed parser and do proper tests.
  @Test
  def moduleMembers {
    assertEquals(
      List("value privateByDefault is not a member of object Foo"),
      compile("""
        object Foo {
          val privateByDefault = 10
          @public val explicitlyPublic = 12
        }

        object Bar {
          println(Foo.explicitlyPublic)
          println(Foo.privateByDefault)
        }
      """)
    )
  }
}
