case class Scalar(x: Int)
case class ScalarVal(x: Int) extends AnyVal

object ScalarTest {
  def main(args: Array[String]) {

    val s = Scalar(10)
    val Scalar(x) = s

    val sv = ScalarVal(10)
    val ScalarVal(xv) = sv
  }
}
