package example

// run examples/example4.scala -Xprint:scalaxy-streams
object Example4 extends App {
  // println(for (p <- (20 until 0 by -2).zipWithIndex) yield p.toString)

  // println(Some(1).map(_ * 2).filter(_ < 3))
  //(1 to 3).map(_ * 2).filter(_ < 3)
  // Seq(0, 1, 2, 3).map(_ * 2).filter(_ < 3)
  // val s = "1"
  // Option(s).flatMap(s => Option(s).filter(_ != null))

  // val array = (1 to 3).map(i => (i, i * 10)).toArray
  // for (((x, y), i) <- array.zipWithIndex; if (x + y) % 2 == 0) { println(s"array[$i] = ($x, $y)") }

  // val n = 20;
  // println(
  //     for (i <- 0 to n;
  //          ii = i * i;
  //          j <- i to n;
  //          jj = j * j;
  //          if (ii - jj) % 2 == 0;
  //          k <- (i + j) to n)
  //       yield { (ii, jj, k) })
  // for ((v, i) <- (20 until 0 by -2).zipWithIndex) yield (v + i)
  val start = 10
  val end = 100
  println(
    (for (i <- start to end by 2) yield
          (() => (i * 2))
      ).map(_())
  )
}
