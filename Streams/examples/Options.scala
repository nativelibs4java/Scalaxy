import scalaxy.streams.optimize

object Options_Opt extends App {

  val n = 10000000
  val a: Array[Option[Int]] = Array.tabulate(n)(i => if (i % 2 == 0) None else Some(i * i))

  while (true) optimize {
    println(
      (for ((o, i) <- a.zipWithIndex; v <- o) yield v * v - i).sum)
  }
}

object Options_NotOpt extends App {

  val n = 10000000
  val a: Array[Option[Int]] = Array.tabulate(n)(i => if (i % 2 == 0) None else Some(i * i))

  while (true) {
    println(
      (for ((o, i) <- a.toIterator.zipWithIndex; v <- o) yield v * v - i).sum)
  }
}
