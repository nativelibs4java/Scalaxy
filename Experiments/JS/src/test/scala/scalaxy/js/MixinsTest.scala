package scalaxy.js

import org.junit._
import Assert._

class MixinsTest {
  trait A { def foo = "A" }
  trait B { def foo = "B" }

  @Test
  def simple {
    // class C extends A with B
    assertEquals("A",
      (new A {}).foo)

    assertEquals("super = A",
      (new A { override def foo = "super = " + super.foo }).foo)

    assertEquals("C",
      (new A with B { override def foo = "C" }).foo)

    assertEquals("super = A",
      (new A with B { override def foo = "super = " + super[A].foo }).foo)

    assertEquals("super = B",
      (new A with B { override def foo = "super = " + super[B].foo }).foo)
  }
}
