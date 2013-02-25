/*
; run examples/Generics.scala -Xprint:scalaxy-extensions -Xplugin:/Users/ochafik/.ivy2; run examples/GenericsTest.scala -Xprint:refchecks

sbt "project scalaxy-loops" "publish-local"
sbt "project scalaxy-extensions" "run examples/Generics.scala" "run examples/GenericsTest.scala -Xprint:refchecks"

*/
package scalaxy.examples

import scalaxy.loops._
import scala.language.postfixOps // Optional.

object GenericsTest extends App
{
  {
    import scalaxy.ExampleAlgo._
    
    val v = 10
    println(10.divAddMul(2, 3, 4))
    println(v.divAddMul(2, 3, 4))
  }
  
  {
    import scalaxy.Matrices._
    
    val a = Matrix[Int](3, 3)
    a(0, 0) = 2
    a(1, 1) = 1
    a(2, 2) = 1
    
    val b = Matrix[Int](3, 3)
    b(0, 0) = 2
    b(1, 1) = 2
    b(2, 2) = 1
    println(a)
    println(b)
    
    val c: Matrix[Int] = a * b
    println(c)
    
    val bb = Matrix[Int](3, 1)
    println(a * bb)
    
    val bbb = Matrix[Int](2, 3)
    println(a * bbb)
  }
}
