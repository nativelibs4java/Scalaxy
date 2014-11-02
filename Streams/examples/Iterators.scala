import scalaxy.streams.optimize

object Iterators_Opt extends App {

  val n = 10000000
  val a: Array[Int] = Array.tabulate(n)(i => i * i)

  while (true) optimize {
    println(
      (for ((v, i) <- a.zipWithIndex; if i % 2 == 0) yield v * v - i).sum)
  }
}

object Iterators_NotOpt extends App {

  val n = 10000000
  val a: Array[Int] = Array.tabulate(n)(i => i * i)

  while (true) {
    println(
      (for ((v, i) <- a.toIterator.zipWithIndex; if i % 2 == 0) yield v * v - i).sum)
  }
}
