package scalaxy.extensions
package test

import org.junit._
import Assert._

class SimpleTest extends TestBase {
  @Test
  def simple {
    val original = """
      object O {
        @scalaxy.extend(Int) def foo: String = self + "foo"
      }
    """
    
    val equivalent = """
      object O {
        import scala.language.experimental.macros;
        implicit class scalaxy$extensions$foo$1(self: Int) extends scala.AnyVal {
          def foo: String = macro scalaxy$extensions$foo$1.foo
        }
        object scalaxy$extensions$foo$1 {
          def foo(c: scala.reflect.macros.Context): c.Expr[String] = {
            import c.universe._
            val Apply(_, List(selfTree1)) = c.prefix.tree
            val self = c.Expr[Int](selfTree1)
            reify(self.splice + "foo")
          }
        }
      }
    """
    
    val (expected, _) = transform(equivalent)
    val (actual, _) = transform(original)
    assertEquals(expected, actual)
  }
}
