package scalaxy.extensions
package test

import org.junit._
import Assert._

class SimpleTest extends TestBase {
  def trans(s: String, name: String = "test"): String = {
    val (res, _) :: Nil = transform(List(s), name)
    res
  }
  @Test
  def noReturnType {
    trans("object O { @scalaxy.extend(Int) def foo: Int = 10 }")
    try {
      trans("object O { @scalaxy.extend(Int) def foo = 10 }")
      fail("Code should not have compiled, was missing return type")
    } catch { case ex: Throwable => }
  }
  @Test
  def simple {
    val original = """
      object O {
        @scalaxy.extend(Int) def foo(quote: String): String = quote + self + quote
      }
    """
    
    val equivalent = """
      object O {
        import scala.language.experimental.macros;
        implicit class scalaxy$extensions$foo$1(val self: Int) extends scala.AnyVal {
          def foo(quote: String): String = macro scalaxy$extensions$foo$1.foo
        }
        object scalaxy$extensions$foo$1 {
          def foo(c: scala.reflect.macros.Context)(quote: c.Expr[String]): c.Expr[String] = {
            import c.universe._
            val Apply(_, List(selfTree1)) = c.prefix.tree
            val self = c.Expr[Int](selfTree1)
            reify(quote.splice + self.splice + quote.splice)
          }
        }
      }
    """
    
    assertEquals(trans(equivalent, "equiv"), trans(original, "orig"))
  }
}
