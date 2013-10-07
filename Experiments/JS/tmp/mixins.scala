trait A { def foo = "A" }
trait B { def foo = "B" }

class MixedA extends A
class MixedSuperA extends A { override def foo = "super = " + super.foo }
class MixedSuperAB_A extends A with B { override def foo = "super = " + super[A].foo }
class MixedSuperAB_B extends A with B { override def foo = "super = " + super[B].foo }

object Mixins {
  println("mix!")
  val x = 10
  lazy val y = {
    println("Got y!")
    12
  }
}
