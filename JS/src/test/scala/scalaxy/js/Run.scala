package scalaxy.js

@JavaScript
@global
object Run {

  println("This is run directly!")

  class Sub {
    println("Creating a sub class")
  }
  println(new Sub)

  // class Sub(val x: Int) {
  //   println("Creating a sub class with x = " + x)
  // }
  // println(new Sub(10))
}
