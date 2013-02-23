/*
run examples/Complex.scala -Xprint:scalaxy-extensions
run examples/ComplexTest.scala
*/

// TODO hygienize self and params (by value, not by name as currently)
object Run extends App {
  import ComplexImplicits._
  
  val x = Complex(1, 0)
  val y = Complex(0, 1)
  
  println(x * (x + y))
  //println(x * y)
  //println(y * y)
}
