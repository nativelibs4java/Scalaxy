package example

object Example2 {
  def main(args: Array[String]): Unit = {
    // val n = 20;
    // println {
    //   for (i <- 0 to n;
    //        ii = i * i;
    //        j <- i to n;
    //        jj = j * j;
    //        if (ii - jj) % 2 == 0;
    //        k <- (i + j) to n)
    //     yield { (ii, jj, k) }
    // }
    

    // val col = (0 to 10).toList
    // val arr = col.map(_ * 2).toArray
    //(0 to 10).map(_ * 2).toList
    //
    // (0 to 10).map(_ * 2).toArray

    // println((0 to 1000).filter(v => (v % 2) == 0).map(_ * 2).toArray.toSeq)

    //println(List(1, 2, 3).flatMap(v => List(v, v + 1)).count(_ % 2 == 0))
    // println(List(1, 2) collect {
    //   case x if x > 1 =>
    //     x - 1
    //   case x =>
    //     x
    // } map (_ + 1))

    import scalaxy.streams.optimize
    
    optimize {
      case class Foo(i: Int)
      val arr = new Array[Foo](5)
      for (Foo(i) <- arr) yield i
    }

    // val value = List(1, 2)
    
    // Option(value) collect {
    //   case List(a, b) =>
    //     print(a)
    //   case Nil =>
    //     print("haa")
    // }

    // val a = Array(1, 2)
    // val b = new collection.mutable.ArrayBuilder.ofInt()
    // var i = 0
    // val n = a.length
    // while (i < n) {
    //   val item = a(i)
    //   var found = false
    //   var value = 0
    //   item match {
    //     case x if x > 1 =>
    //       found = true;
    //       value = x - 1;
    //     case x =>
    //       found = true;
    //       value = x;
    //   }
    //   if (found) {
    //     b += value
    //   }
    //   i += 1
    // }
    
    // println(List(1, 2, 3).flatMap(v => List(v * 2, v * 2 + 1).map(_ + 1)).count(_ % 2 == 0))
    // println(List(1, 2, 3).flatMap(v => List(v * 2, v * 2 + 1)).dropWhile(_ < 3))
    // println(List(1, 2, 3).flatMap(v => List(v * 2, v * 2 + 1).map(_ + 1)).dropWhile(_ < 3))

    // (0 to 10).toList
    // val n = 5
    // println(for (i <- 0 until n; v <- (i to 0 by -1).toArray; j <- 0 until v) yield {
    //   (i, v, j)
    // })
    // print(arr.mkString)
  }
}
