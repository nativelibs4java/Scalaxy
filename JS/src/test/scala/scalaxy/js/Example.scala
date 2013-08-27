package scalaxy.js.example

import scalaxy.js._

@JavaScript
object Example {
  import Global._

  println("hello, world")
  println("hello, world, again")
  window.alert("I'm here")
  lazy val iAmLazy = {
    println("You've called me!")
    10
  }

  val someProperty = "fooo"
}

@global
object Main {
  println("This is run directly!")
  class Sub {
    println("Creating a sub class")
  }
  println(new Sub)
}
