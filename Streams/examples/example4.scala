
object Example4 extends App {
  // println(for (p <- (20 until 0 by -2).zipWithIndex) yield p.toString)

  println(Some(1).map(_ * 2).filter(_ < 3))
}
