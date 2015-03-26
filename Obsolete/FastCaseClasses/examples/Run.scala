
object Run extends App {
  case class Scalar(x: Int)
  case class Tup(x: Int, y: Int)

  val n = 10000000
  val a = Array.tabulate(n)(i => new Tup(i, i + 1))

  while (true) {

    {
      var tot = 0L
      var i = 0
      while (i < n) {
        val Tup(x, y) = a(i)
        tot += x + y
        i += 1
      }
      println(tot)
    }
  }
}

object Run_NoFastCaseClasses extends App {
  case class Scalar_NoFastCaseClasses(x: Int)
  case class Tup_NoFastCaseClasses(x: Int, y: Int)
  
  val n = 10000000
  val a = Array.tabulate(n)(i => new Tup_NoFastCaseClasses(i, i + 1))

  while (true) {

    {
      var tot = 0L
      var i = 0
      while (i < n) {
        val Tup_NoFastCaseClasses(x, y) = a(i)
        tot += x + y
        i += 1
      }
      println(tot)
    }
  }
}
