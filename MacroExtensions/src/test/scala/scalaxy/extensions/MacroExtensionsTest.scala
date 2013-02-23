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
              val Apply(_, List(selfTree$1)) = c.prefix.tree;
              val self$Expr$1: c.Expr[String] = c.Expr[String](selfTree$1)
              reify({
                val self: String = self$Expr$1.splice
                self.length
              })
            }
          }
        }
      """
    )
  }

  @Test
  def oneByValueArg {
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
            def foo(quote$Expr$1: String): String = 
              macro scalaxy$extensions$foo$1.foo
          }
          object scalaxy$extensions$foo$1 {
            def foo(c: scala.reflect.macros.Context)
                   (quote$Expr$1: c.Expr[String]): 
                c.Expr[String] = 
            {
              import c.universe._
              val Apply(_, List(selfTree$1)) = c.prefix.tree;
              val self$Expr$1: c.Expr[Int] = c.Expr[Int](selfTree$1)
              reify({
                val self: Int = self$Expr$1.splice
                val quote: String = quote$Expr$1.splice
                quote + self + quote
              })
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
            def foo[A](a$Expr$1: A): A = 
              macro scalaxy$extensions$foo$1.foo[A]
          }
          object scalaxy$extensions$foo$1 {
            def foo[A : c.WeakTypeTag]
                   (c: scala.reflect.macros.Context)
                   (a$Expr$1: c.Expr[A]):
                c.Expr[A] = 
            {
              import c.universe._
              val Apply(_, List(selfTree$1)) = c.prefix.tree;
              val self$Expr$1: c.Expr[Double] = c.Expr[Double](selfTree$1)
              reify({
                val self: Double = self$Expr$1.splice;
                val a: A = a$Expr$1.splice;
                {
                  println(s"${self}.foo(${a})")
                  a
                }
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
            def foo[B](b$Expr$1: B): (Array[A], B) = 
              macro scalaxy$extensions$foo$1.foo[A, B]
          }
          object scalaxy$extensions$foo$1 {
            def foo[A : c.WeakTypeTag, B : c.WeakTypeTag]
                   (c: scala.reflect.macros.Context)
                   (b$Expr$1: c.Expr[B]):
                c.Expr[(Array[A], B)] = 
            {
              import c.universe._
              val Apply(_, List(selfTree$1)) = c.prefix.tree;
              val self$Expr$1: c.Expr[Array[A]] = c.Expr[Array[A]](selfTree$1)
              reify({
                val self: Array[A] = self$Expr$1.splice
                val b: B = b$Expr$1.splice
                (self, b)
              })
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
          @scalaxy.extend(T) def squared[T : Numeric]: T = self * self
        }
      """,
      """
        object O {
          import scala.language.experimental.macros;
          implicit class scalaxy$extensions$squared$1[T](val self: T)
          extends scala.AnyRef {
            def squared(implicit evidence$1$Expr$1: Numeric[T]): T = 
              macro scalaxy$extensions$squared$1.squared[T]
          }
          object scalaxy$extensions$squared$1 {
            def squared[T](c: scala.reflect.macros.Context)
                          (evidence$1$Expr$1: c.Expr[Numeric[T]])
                          (implicit evidence$2: c.WeakTypeTag[T]): 
                c.Expr[T] = 
            {
              import c.universe._
              val Apply(_, List(selfTree$1)) = c.prefix.tree;
              val self$Expr$1: c.Expr[T] = c.Expr[T](selfTree$1)
              reify({
                val self: T = self$Expr$1.splice
                implicit val evidence$1: Numeric[T] = evidence$1$Expr$1.splice
                self * self
              })
            }
          }
        }
      """
    )
  }
  
  @Test
  def passImplicitsThroughToMacro {
    assertSameTransform(
      """
        object O {
          @scalaxy.extend(T) 
          def squared[T : Numeric]: T = macro {
            val evExpr = implicity[c.Expr[Numeric[T]]]
            reify({
              implicit val ev = evExpr.splice
              self * self
            })
          }
        }
      """,
      """
        object O {
          import scala.language.experimental.macros;
          implicit class scalaxy$extensions$squared$1[T](val self: T)
          extends scala.AnyRef {
            def squared(implicit evidence$1$Expr$1: Numeric[T]): T = 
              macro scalaxy$extensions$squared$1.squared[T]
          }
          object scalaxy$extensions$squared$1 {
            def squared[T](c: scala.reflect.macros.Context)
                          (evidence$1$Expr$1: c.Expr[Numeric[T]])
                          (implicit evidence$2: c.WeakTypeTag[T]): 
                c.Expr[T] = 
            {
              import c.universe._
              val Apply(_, List(selfTree$1)) = c.prefix.tree;
              val self: c.Expr[T] = c.Expr[T](selfTree$1);
              {
                implicit def evidence$1$1: c.Expr[Numeric[T]] = c.Expr[Numeric[T]](evidence$1$Expr$1);
                {
                  val evExpr = implicity[c.Expr[Numeric[T]]]
                  reify({
                    implicit val ev = evExpr.splice
                    self * self
                  })
                }
              }
            }
          }
        }
      """
    )
  }

  @Test
  def oneByNameArgWithImplicitClassTag {
    assertSameTransform(
      """
        import scala.reflect.ClassTag
        object O {
          @scalaxy.extend(Int) 
          def fill[T : ClassTag](generator: => String): Array[Array] = 
            Array.fill[T](self)(generator)
        }
      """,
      """
        import scala.reflect.ClassTag
        object O {
          import scala.language.experimental.macros;
          implicit class scalaxy$extensions$fill$1(val self: Int) 
          extends scala.AnyVal {
            def fill[T](generator: String)(implicit evidence$1$Expr$1: ClassTag[T]): Array[Array] = 
              macro scalaxy$extensions$fill$1.fill[T]
          }
          object scalaxy$extensions$fill$1 {
            def fill[T](c: scala.reflect.macros.Context)
                       (generator: c.Expr[String])
                       (evidence$1$Expr$1: c.Expr[ClassTag[T]])
                       (implicit evidence$2: c.WeakTypeTag[T]): 
                c.Expr[Array[Array]] = 
            {
              import c.universe._
              val Apply(_, List(selfTree$1)) = c.prefix.tree;
              val self$Expr$1: c.Expr[Int] = c.Expr[Int](selfTree$1)
              reify({
                val self: Int = self$Expr$1.splice
                implicit val evidence$1: ClassTag[T] = evidence$1$Expr$1.splice
                Array.fill[T](self)(generator.splice)
              })
            }
          }
        }
      """
    )
  }

  @Test
  def oneByNameAndOneByValueArg {
    assertSameTransform(
      """
        object O {
          @scalaxy.extend(Int) 
          def fillZip(value: Int, generator: => String): Array[(Int, String)] = 
            Array.fill(self)((value, generator))
        }
      """,
      """
        object O {
          import scala.language.experimental.macros;
          implicit class scalaxy$extensions$fillZip$1(val self: Int) 
          extends scala.AnyVal {
            def fillZip(value$Expr$1: Int, generator: String): Array[(Int, String)] = 
              macro scalaxy$extensions$fillZip$1.fillZip
          }
          object scalaxy$extensions$fillZip$1 {
            def fillZip(c: scala.reflect.macros.Context)
                       (value$Expr$1: c.Expr[Int], generator: c.Expr[String]): 
                c.Expr[Array[(Int, String)]] = 
            {
              import c.universe._
              val Apply(_, List(selfTree$1)) = c.prefix.tree;
              val self$Expr$1: c.Expr[Int] = c.Expr[Int](selfTree$1)
              reify({
                val self: Int = self$Expr$1.splice
                val value: Int = value$Expr$1.splice
                Array.fill(self)((value, generator.splice))
              })
            }
          }
        }
      """
    )
  }
}
