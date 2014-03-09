object Example3 extends App {
  val n = 20;
  for ((v, i) <- (0 to n).zipWithIndex) {
    println(v + i)
  }
}
