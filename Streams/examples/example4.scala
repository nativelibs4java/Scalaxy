// run examples/example4.scala -Xprint:scalaxy-streams
object Example4 extends App {
  // println(for (p <- (20 until 0 by -2).zipWithIndex) yield p.toString)

  // println(Some(1).map(_ * 2).filter(_ < 3))
  //(1 to 3).map(_ * 2).filter(_ < 3)
  // Seq(0, 1, 2, 3).map(_ * 2).filter(_ < 3)
  // val s = "1"
  // Option(s).flatMap(s => Option(s).filter(_ != null))

  val array = (1 to 3).map(i => (i, i * 10)).toArray
  for (((x, y), i) <- array.zipWithIndex; if (x + y) % 2 == 0) { println(s"array[$i] = ($x, $y)") }

  // for ((v, i) <- (20 until 0 by -2).zipWithIndex) yield (v + i)
}
