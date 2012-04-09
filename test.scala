//object RunMe extends App {
class RunMe {
  
  {
    val i = 10
    val s = i.toString
    println(s)
  }
  
  def trans(col: Seq[Int]) = {
    col.map(_ + 1).map(_ * 2)
  }
  
  def trans(col: Seq[Int], v: Int) = {
    for (i <- 1 until 10)
      println("i = " + i + " // v = " + v) 
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
  
  */
  println(trans(Seq(1, 2, 3)))
  println(trans(Seq(2, 3, 4), 10))
  
}
