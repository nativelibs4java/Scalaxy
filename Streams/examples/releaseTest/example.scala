package example

object Example1 extends App {
  import scalaxy.streams.optimize

  // import scalaxy.streams.strategy.safer
  // import scalaxy.streams.strategy.safe
  // import scalaxy.streams.strategy.aggressive
  import scalaxy.streams.strategy.safe

  // val n = 10

  // def outsider[A](a: A) = a

  // optimize {
  //   // println((0 to 10).map(i => (i, i)))

  //   (0 to n)
  //     .foreach(i => {
  //       println(i)
  //     })
  //   // print((0 to 10).toList)
  //   // print((0 to 10).map(outsider))
  //   // print((0 to 10).map(outsider).map(_ + 2))
  //   // print((0 to 10).map(outsider).map(_.toString + new Object().toString).map(outsider))
  // }


  case class Ident(name: String)
  def example = {
    val a = new Array[Ident](10)
    optimize {
      for (Ident(name) <- a) {
        println(name)
      }
    }
  }
}
