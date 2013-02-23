/*
run examples/TestExtensions.scala -Xprint:scalaxy-extensions
run examples/Test.scala
*/
object TestExtensions
{
  import scala.language.experimental.macros
  import scala.reflect.ClassTag
  /*
  @scalaxy.extension[Any] def faulty {
    val self3 = 10
    println(self3)
  }
  */
  //@scalaxy.extension[Array[T]] def notNulls[T <: AnyRef]: Int = self.count(_ ne null)

  //@scalaxy.extension[Array[T]] def notNulls[T]: Int = 10

  
  
  @scalaxy.extension[Int] 
  def copiesOf[T : ClassTag](generator: => T): Array[T] = 
    Array.fill[T](self)(generator)
  
  //@scalaxy.extension[Array[A]] def foo[A, B](b: B): (Array[A], B) = (self, b)
  /*
  @scalaxy.extension[Array[A]] def tup[A, B](b: B): (A, B) = macro {
    println("Extension macro is executing!") 
    reify((self.splice.head, b.splice))
  }
  */
  
  /*
  @scalaxy.extension[Array[Int]] def inotNulls: Int =
    self.count(_ != 0)

  @scalaxy.extension[Int] def str0 { println(self.toString) }

  @scalaxy.extension[Any] def quoted(quote: String): String = quote + self + quote

  @scalaxy.extension[Int] def str1: String = self.toString

  @scalaxy.extension[Int] def str2: String = macro
  {
    println("EXECUTING EXTENSION MACRO!")
    reify(self.splice.toString)
  }
  */

  /*
  @scalaxy.extension[Int] def str = macro reify(self.splice.toString)
  @scalaxy.extension[Int] def str = macro {
    ...
    reify(self.splice.toString)
  }
  */
  //println(10.str)
}
