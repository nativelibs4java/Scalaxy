
object Example1 extends App {
  // import scalaxy.loops.optimize

  val n = 10

  // optimize { 10 }
  
  {
    for (i <- 0 to n) {
      println(i)
    }
  }

  println {
    for (i <- 0 to n) yield {
      i + 2
    }
  }

  println {
    for (i <- 0 to n; if i % 2 == 1) yield {
      i + 2
    }
  }

  println {
    for (i <- 0 to n; j <- i to 1 by -1; if i % 2 == 1) yield { i + j }
  }
}
