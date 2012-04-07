//object RunMe extends App {
class RunMe {
  def trans(col: Seq[Int]) = {
    col.map(_ + 1).map(_ * 2)
  }
  /*def transManual(col: Seq[Int]) = {
    col.map(a => {
      val b = ((a:Int) => a + 1)(a)
      val c = ((a:Int) => b + 1)(b)
      c
    })
  }
  def run = {
    for (i <- 0 until 100) println(i + "...")
  }
  
  run
  
  {
    val i = 10
    val s = i.toString
    println(s)
  }
  */
  println(trans(Seq(1, 2, 3)))
  
}
