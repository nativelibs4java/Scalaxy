object Test extends App {
  import TestExtensions._

  //println(10.str1)
  //println(11.str2)
  var i = 0
  println((10 copiesOf { i += 1; (i, 'same) }).toSeq)
  //println(Array(3).tup(1.0)) // macro-expanded to `(Array(3).head, 1.0)`  (and prints a message during compilation)
}
