package example

object Example5 {
  def doIt(name: String) {
    val n = Option(name).filter(_.startsWith("_")).orNull
    for (nn <- Option(n)) {
      println(nn)
    }

    print(List(1, 2, 3).flatMap(x => List(x + 1, x + 2)).filter(_ < 2).mkString)
    //    1                     1    3                   1      1      (1)
  }
}
