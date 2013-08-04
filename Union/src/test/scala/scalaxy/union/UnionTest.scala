package scalaxy.union.test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror

import scalaxy.union._

//@Ignore
class UnionTest {

  //def callMe[N <:< (Int | Long | Float | Double)](n: N) = {
  // def callMe[N <:< Int](n: N) = {
  def callMe[N](n: N)(implicit ev: N =:= (Int | Long | Float | Double)) = {
    println("yay: " + n)
    //if (ev.is[Int])
  }

  @Test
  def testUnion {
    // implicitly[Int <:< Int]
    // implicitly[(Int | Float) <:< (Int | Float)]

    // typeIsSubtypeOfUnion[Int, Int <:< Float]
    // typeIsSubtypeOfUnion[Int, Int | Float]
    // implicitly[Int <:< (Int | Float)]

    callMe(10)
    // callMe(10L)
  }
}

