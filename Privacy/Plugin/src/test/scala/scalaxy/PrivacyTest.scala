package scalaxy.privacy.test

import scala.language.existentials

import scala.tools.nsc.Global
import scalaxy.privacy.PrivacyComponent
import scala.tools.nsc.reporters.{ StoreReporter, Reporter }
import org.junit._
import org.junit.Assert._

class PrivacyTest extends TestBase {

  override def getInternalPhases(global: Global) =
    List(new PrivacyComponent(global))

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
      List(
        "value privateByDefault is not a member of object Foo"
      ),
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
  @Test
  def locals {
    assertEquals(
      Nil,
      compile("""
        @public object Foo {
          def f {
            val v = 10
            def ff: Int = v

            () => { v + ff }
          }
        }
      """)
    )
  }
  @Test
  def abstracts {
    assertEquals(
      Nil,
      compile("""
        @public trait A {
          def f: Int
        }
        abstract class B {
          def g: Int
        }
        @public class C extends B with A {
          override def f = 10
          override def g = 10
        }
        @public object Foo {
          val c = new C
          println(c.f)
          println(c.g)
        }
      """)
    )
  }
  @Test
  def diffs {
    assertEquals(
      Nil,
      compile("""
        @public object Foo { val x = 10 }
        class Bar(x: Int, y: Int) { val z = 10 }
      """)
    )
  }
}
