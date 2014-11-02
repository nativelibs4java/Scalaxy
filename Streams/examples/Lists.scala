import scalaxy.streams.optimize

object Lists_Opt extends App {

  val n = 10000000
  val a: List[Int] = Array.tabulate(n)(i => i * i).toList

  while (true) optimize {
    println(
      (for ((v, i) <- a.zipWithIndex; if i % 2 == 0) yield v * v - i).sum)
  }
}

object Lists_NotOpt extends App {

  val n = 10000000
  val a: List[Int] = Array.tabulate(n)(i => i * i).toList

  while (true) {
    println(
      (for ((v, i) <- a.zipWithIndex; if i % 2 == 0) yield v * v - i).sum)
  }
}
