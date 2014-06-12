object WikiTest extends App {
	// For instance, given the following array:
	val array = if (args.length == 0) Array(1, 2, 3, 4) else args.map(_.toInt)

	// The following for comprehension:
	for ((item, i) <- array.zipWithIndex; if item % 2 == 0) {
	  println(s"array[$i] = $item")
	}
}
