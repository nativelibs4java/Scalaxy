/*
; run examples/Generics.scala -Xprint:scalaxy-extensions ; run examples/GenericsTest.scala -Xprint:refchecks

sbt "project scalaxy-extensions" "run examples/Generics.scala" "run examples/GenericsTest.scala -Xprint:refchecks"

*/
package scalaxy.examples
object GenericsTest extends App
{
  //import scalaxy.generics._
  import scalaxy.GenericsAlgo._
  
  val v = 10
  println(10.divAddMul(2, 3, 4))
  println(v.divAddMul(2, 3, 4))
  
  //def typed[T : Numeric](v: T, div: T, add: T, mul: T): T = {
  //  import scala.math.Numeric.Implicits._
  //  import scala.math.Ordering.Implicits._
  //  v.divAddMul(div, add, mul)
  //}
  //
  //println(typed(v, 2, 3, 4))
}
