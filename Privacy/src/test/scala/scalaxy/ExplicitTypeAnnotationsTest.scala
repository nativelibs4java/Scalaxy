package scalaxy.privacy.test

import scala.language.existentials

import scala.tools.nsc.Global

import scalaxy.privacy.ExplicitTypeAnnotationsComponent
import scala.tools.nsc.reporters.{ StoreReporter, Reporter }
import org.junit._
import org.junit.Assert._

object ExplicitTypeAnnotationsTest {
  def shouldHaveAnnotationMsg(name: String) =
    s"Public member `$name` with non-trivial value should have an explicit type annotation"
}
class ExplicitTypeAnnotationsTest extends TestBase {

  override def getInternalPhases(global: Global) =
    List(new ExplicitTypeAnnotationsComponent(global))

  import ExplicitTypeAnnotationsTest.shouldHaveAnnotationMsg

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
  def trivials {
    assertEquals(
      Nil,
      compile("""
        class Foo {
          def nullaryFunctionWithStringPlusNumber = "blah" + 10.0
          def valWithNumberTimesNumber = 10.0 * 20.0

          def interpolatedString = s"blah $valWithNumberTimesNumber"

          def list = List(1, 2)
          def set = Set("1", "2")
          val seq = Seq(1, 2)
          def array = Array(1, 2)
          def iterable = Iterable(1, 2)
          def traversable = Traversable(1, 2)

          val arrayWithType = Array[Int]()
          val listWithType = List[(String, Long)]()
          val mapWithTypes = Map[Int, (Double, Float)]()

          override def toString = if (arrayWithType.length > 10) "1" else "2"
        }
      """)
    )
  }
  @Test
  def almostTrivials {
    assertEquals(
      List(
        shouldHaveAnnotationMsg("nullaryFunctionWithNumberPlusString"),
        shouldHaveAnnotationMsg("valWithNumberPlusString"),
        shouldHaveAnnotationMsg("heterogeneousList"),
        shouldHaveAnnotationMsg("heterogeneousArray"),
        shouldHaveAnnotationMsg("nestedArray")
      ),
      compile("""
        class Foo {
          def nullaryFunctionWithNumberPlusString = 10 + "blah"
          val valWithNumberPlusString = 10 + "blah"

          def heterogeneousList = List(1, "2")
          val heterogeneousArray = Array(1, "2")
          def nestedArray = Array(Set[Int](), Set[Int]())
        }
      """)
    )
  }

  @Test
  def nonTrivials {
    assertEquals(
      List(
        shouldHaveAnnotationMsg("functionWithArgWithBranch"),
        shouldHaveAnnotationMsg("nullaryFunctionWithCall"),
        shouldHaveAnnotationMsg("valWithBranch"),
        shouldHaveAnnotationMsg("valWithRef")
      ),
      compile("""
        class Foo {

          def functionWithArgWithBranch(x: Int) = if (x < 10) 1 else 2
          def nullaryFunctionWithCall = functionWithArgWithBranch(10)

          val valWithBranch = if (nullaryFunctionWithCall == 1) 1 else "blah"
          val valWithRef = valWithBranch
        }
      """)
    )
  }
}
