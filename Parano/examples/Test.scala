object Test {
  case class Foo(theFirst: Int, second: Int)

  val foo = Foo(10, 12)                // Error: unnamed params theFirst and second have same type
                                       //        and are ambiguous.
  val foo2 = Foo(10, second = 12)      // Fine.
  val foo3 = Foo(theFirst = 10, 12)    // Fine.

  val Foo(someFirst, someSecond) = foo // Fine.

  val Foo(theSecond, first) = foo      // Error: theSecond used to extract Foo.theFirst,
                                       //        first used to extract Foo.second

  val foo5 = Foo(theSecond, first)     // Error: ident theSecond used for param theFirst,
                                       //        ident first used for param second
}
