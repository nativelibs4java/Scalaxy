package scalaxy.privacy.test

import scala.language.existentials

import scala.tools.nsc.Global

import scalaxy.privacy.ExplicitTypeAnnotationsComponent
import scala.tools.nsc.reporters.{ StoreReporter, Reporter }
import org.junit._
import org.junit.Assert._

class ExplicitTypeAnnotationsTest extends TestBase {

  override def getInternalPhases(global: Global) =
    List(new ExplicitTypeAnnotationsComponent(global))

  @Test
  def allGood {
    assertEquals(
      Nil,
      compile("""
        class Foo {
          def f = 10
          def ff: Int = f

          val v = 10
          val vv: Int = v
        }
      """)
    )
  }

  @Test
  def trivial {
    assertEquals(
      Nil,
      compile("""
        class Foo {
          def f = 10 + "blah"
          val v = 10 + "blah"
        }
      """)
    )
  }

  @Test
  def allBad {
    assertEquals(
      List(
        "Public member `f` with non-trivial value should have a explicit type annotation",
        "Public member `ff` with non-trivial value should have a explicit type annotation",
        "Public member `v` with non-trivial value should have a explicit type annotation",
        "Public member `vv` with non-trivial value should have a explicit type annotation"
      ),
      compile("""
        class Foo {
          def f(x: Int) = if (x < 10) 1 else 2
          def ff = f(10)

          val v = if (ff == 1) 1 else "blah"
          val vv = v
        }
      """)
    )
  }
}
