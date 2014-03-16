/*
~/bin/scala-2.11.0-M8/bin/scalac -Xprint:delambdafy -optimise -Yclosure-elim -Yinline examples/example3.scala
*/
// import scalaxy.loops._

object Example3 {
  def main(args: Array[String]) {
    val n = 20;
    for (v <- 0 to n) yield v
    // for (v <- 0 to n) println(v)
    // optimized {
    // }
    // for ((v, i) <- (0 to n).zipWithIndex) {
    //   println(v + i)
    // }
    // println((1 to n).map(_ * 2).toList)
    // println((1 to n).map(_ * 2).toArray)
    // println((1 to n).map(_ * 2).toSet)
  }
}
