@public object Test {
  case class Foo(theFirst: Int, second: Int) {
    @public val pubVal = theFirst + second
    val privVal = pubVal
  }

  val foo = Foo(10, 12)
  println(foo.theFirst)
  println(foo.second)
  println(foo.pubVal)
  println(foo.privVal)
}
