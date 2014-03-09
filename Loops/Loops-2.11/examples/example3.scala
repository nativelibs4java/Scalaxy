object Example3 extends App {
  val n = 20;
  for ((v, i) <- (0 to n).zipWithIndex) {
    println(v + i)
  }
  // println((1 to n).map(_ * 2).toList)
  // println((1 to n).map(_ * 2).toArray)
  // println((1 to n).map(_ * 2).toSet)
}
