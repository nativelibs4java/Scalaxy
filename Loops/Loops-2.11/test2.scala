import scala.reflect.ClassTag
object Test2 {
  def main(args: Array[String]) {
    val arrays = Array(Array(1, 2), Array(3, 4))

    
    var i = 0
    val length1 = arrays.length
    val b = Array.canBuildFrom[(Int, Int)](implicitly[ClassTag[(Int, Int)]])()
    while (i < length1) {
      val array = arrays(i)
      val length = array.length
      if (length < 10) {
        var j = 0
        val length2 = array.length
        while (j < length2) {
          val v = array(j)
          b += ((length, v))
          j += 2
        }
      }
      i += 1
    }
    b.result()
  }
}
