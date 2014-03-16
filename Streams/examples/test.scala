object Test {
  def main(args: Array[String]) {
    val arrays = Array(Array(1, 2), Array(3, 4))

    for (array <- arrays;
         length = array.length * 30;
         if length < 10;
         v <- array)
      yield
        (length, v)
  }
}
