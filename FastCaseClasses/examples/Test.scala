object Test extends App {
  case class Papa(x: Int) {
    case class Fiston(y: Int)
  }
  // case class Foo(x: Int)
  trait Bar {
    def a: Int
    def c = a * 100
  }
  case class Foo(a: Int, b: Int, bar: Bar = null) extends Bar

  case class Baz()
  object Baz

  case class Bam() {
    override def toString = null
  }
  case class Bat(x: Int) {
    def this(xs: String) = this(xs.toInt)
  }

  trait Beep {}
  object Beep {}

  val foo = Foo(10, 12)
  println(foo)

  val bar: Bar = foo

  val Foo(a, b, _) = foo
  println(a + ", " + b)

  val foo2 = Foo(10, 12)
  println(s"foo.hashCode == foo2.hashCode: ${foo.hashCode == foo2.hashCode} (${foo.hashCode}, ${foo2.hashCode})")
  println(s"foo == foo2: ${foo == foo2}")


  val foo3 = Foo(10, 11)
  println(s"foo.hashCode == foo3.hashCode: ${foo.hashCode == foo3.hashCode} (${foo.hashCode}, ${foo3.hashCode})")
  println(s"foo == foo3: ${foo == foo3}")


  println(foo.copy(a = 0))
  println(foo.copy(b = 0))
  println(foo.copy(a = 0, b = 0))
  println(foo.c)
  println(foo.bar)
}
