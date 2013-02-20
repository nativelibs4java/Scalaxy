package scalaxy.extensions
package test

import org.junit._
import Assert._

class RuntimeExtensionsTest extends TestBase
{
  override def transform(s: String, name: String = "test") =
    transformCode(s, name, macroExtensions = false, runtimeExtensions = true)._1

  @Test
  def trivial {
    transform("class C { @scalaxy.extend(Int) def foo: Int = 10 }")
  }

  @Test
  def noReturnType {
    expectException("return type is missing") {
      transform("class C { @scalaxy.extend(Int) def foo = 10 }")
    }
  }

  @Test
  def notHygienic {
    expectException("self is redefined locally") {
      transform("class C { @scalaxy.extend(Int) def foo = { val self = 10; self } }")
    }
  }

  @Test
  def noArg {
    assertSameTransform(
      """
        class C {
          @scalaxy.extend(String) def len: Int = self.length
        }
      """,
      """
        class C {
          implicit class scalaxy$extensions$len$1(val self: String) extends scala.AnyRef {
            def len: Int = self.length
          }
        }
      """
    )
  }

  @Test
  def noArgVal {
    assertSameTransform(
      """
        class C {
          @scalaxy.extend(Int) def hash: Int = self.hashCode
        }
      """,
      """
        class C {
          implicit class scalaxy$extensions$hash$1(val self: Int) extends scala.AnyVal {
            def hash: Int = self.hashCode
          }
        }
      """
    )
  }

  @Test
  def oneArg {
    assertSameTransform(
      """
        class C {
          @scalaxy.extend(Int) def foo(quote: String): String = quote + self + quote
        }
      """,
      """
        class C {
          implicit class scalaxy$extensions$foo$1(val self: Int) extends scala.AnyVal {
            def foo(quote: String): String = quote + self + quote
          }
        }
      """
    )
  }

  @Test
  def innerOuterTypeParams {
    assertSameTransform(
      """
        class C {
          @scalaxy.extend(Array[A]) def foo[A, B](b: B): (Array[A], B) = (self, b)
        }
      """,
      """
        class C {
          implicit class scalaxy$extensions$foo$1[A](val self: Array[A]) extends scala.AnyRef {
            def foo[B](b: B): (Array[A], B) = (self, b)
          }
        }
      """
    )
  }
}
