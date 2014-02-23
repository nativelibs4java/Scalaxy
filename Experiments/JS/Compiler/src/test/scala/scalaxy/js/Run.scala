package scalaxy.js


@JavaScript
object Collections {
  def doSomething {
    // val emptyArr = Array[Object]()
    val arr = Array(1, 2, 3, 4)
    // val arr2 = new Array[Int](10)

    val emptyObj = Map[String, Any]()
    val obj = Map[String, Any]()
    println(obj)

    val obj1 = Map("x" -> 1, "y" -> Array(1, 2))
    println(obj1)

    val pair = (1, 2)
    val obj2 = Map(pair, "bleh" -> 1)
    println(obj2)

    val obj3 = Map(1 -> 2, 2 -> 3)
    println(obj3)

    val f = (x: Int) => x + 1
    
    println(f)
    println(f(10))
  }
}

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

  // val pair = (1, 2)
  // val obj2 = Map(pair, "bleh" -> 1)
  // println(obj2)
}
