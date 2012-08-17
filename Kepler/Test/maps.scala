object TestMaps extends App {
  val a = Array(1, 2, 3)
  val b = a.map(_ + 1)
  println(b.mkString(", "))
}
