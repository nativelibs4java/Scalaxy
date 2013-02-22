package scalaxy.extensions
package test

import org.junit._
import Assert._

class MacroExtensionsTest extends TestBase
{
  override def transform(s: String, name: String = "test") =
    transformCode(s, name, macroExtensions = true, runtimeExtensions = false)._1

  @Test
  def trivial {
    transform("object O { @scalaxy.extend(Int) def foo: Int = 10 }")
  }

  @Test
  def noReturnType {
    expectException("return type is missing") {
      transform("object O { @scalaxy.extend(Int) def foo = 10 }")
    }
  }

  @Test
  def notHygienic {
    expectException("self is redefined locally") {
      transform("object O { @scalaxy.extend(Int) def foo = { val self = 10; self } }")
    }
  }

  @Test
  def notInModule {
    expectException("not defined in module") {
      transform("class O { @scalaxy.extend(Int) def foo: Int = 10 }")
    }
  }

  @Test
  def noArg {
    assertSameTransform(
      """
        object O {
          @scalaxy.extend(String) def len: Int = self.length
        }
      """,
      """
        object O {
          import scala.language.experimental.macros;
          implicit class scalaxy$extensions$len$1(val self: String)
          extends scala.AnyRef {
            def len: Int = 
              macro scalaxy$extensions$len$1.len
          }
          object scalaxy$extensions$len$1 {
            def len(c: scala.reflect.macros.Context): c.Expr[Int] = {
              import c.universe._
              val Apply(_, List(selfTree1)) = c.prefix.tree
              val self = c.Expr[String](selfTree1)
              reify(self.splice.length)
            }
          }
        }
      """
    )
  }

  @Test
  def oneArg {
    assertSameTransform(
      """
        object O {
          @scalaxy.extend(Int) def foo(quote: String): String = quote + self + quote
        }
      """,
      """
        object O {
          import scala.language.experimental.macros;
          implicit class scalaxy$extensions$foo$1(val self: Int) 
          extends scala.AnyVal {
            def foo(quote: String): String = 
              macro scalaxy$extensions$foo$1.foo
          }
          object scalaxy$extensions$foo$1 {
            def foo(c: scala.reflect.macros.Context)
                   (quote: c.Expr[String]): c.Expr[String] = 
            {
              import c.universe._
              val Apply(_, List(selfTree1)) = c.prefix.tree
              val self = c.Expr[Int](selfTree1)
              reify(quote.splice + self.splice + quote.splice)
            }
          }
        }
      """
    )
  }

  @Test
  def typeParam {
    assertSameTransform(
      """
        object O {
          @scalaxy.extend(Double) def foo[A](a: A): A = {
            println(s"$self.foo($a)")
            a
          }
        }
      """,
      """
        object O {
          import scala.language.experimental.macros;
          implicit class scalaxy$extensions$foo$1(val self: Double)
          extends scala.AnyVal {
            def foo[A](a: A): A = 
              macro scalaxy$extensions$foo$1.foo[A]
          }
          object scalaxy$extensions$foo$1 {
            def foo[A : c.WeakTypeTag]
                (c: scala.reflect.macros.Context)
                (a: c.Expr[A]): c.Expr[A] = 
            {
              import c.universe._
              val Apply(_, List(selfTree1)) = c.prefix.tree
              val self = c.Expr[Double](selfTree1)
              reify({
                println(s"${self.splice}.foo(${a.splice})")
                a.splice
              })
            }
          }
        }
      """
    )
  }

  @Test
  def innerOuterTypeParams {
    assertSameTransform(
      """
        object O {
          @scalaxy.extend(Array[A]) def foo[A, B](b: B): (Array[A], B) = (self, b)
        }
      """,
      """
        object O {
          import scala.language.experimental.macros;
          implicit class scalaxy$extensions$foo$1[A](val self: Array[A])
          extends scala.AnyRef {
            def foo[B](b: B): (Array[A], B) = 
              macro scalaxy$extensions$foo$1.foo[A, B]
          }
          object scalaxy$extensions$foo$1 {
            def foo[A : c.WeakTypeTag, B : c.WeakTypeTag]
                (c: scala.reflect.macros.Context)
                (b: c.Expr[B]): c.Expr[(Array[A], B)] = 
            {
              import c.universe._
              val Apply(_, List(selfTree1)) = c.prefix.tree
              val self = c.Expr[Array[A]](selfTree1)
              reify((self.splice, b.splice))
            }
          }
        }
      """
    )
  }
  
  @Test
  def passImplicitsThrough {
    assertSameTransform(
      """
        object O {
          @scalaxy.extend(T) 
          def squared[T : Numeric]: T = self * self
        }
      """,
      """
        object O {
          import scala.language.experimental.macros;
          implicit class scalaxy$extensions$squared$1[T](val self: T)
          extends scala.AnyRef {
            def squared(implicit evidence$1: Numeric[T]): T = 
              macro scalaxy$extensions$squared$1.squared[T]
          }
          object scalaxy$extensions$squared$1 {
            def squared[T : c.WeakTypeTag]
                (c: scala.reflect.macros.Context)
                (evidence$1: c.Expr[Numeric[T]]): c.Expr[T] = 
            {
              import c.universe._
              val Apply(_, List(selfTree1)) = c.prefix.tree
              val self = c.Expr[Array[A]](selfTree1)
              reify({
                implicit val evidence$1$1 = evidence$1.splice
                self.splice * self.splice
              })
            }
          }
        }
      """
    )
  }
}
