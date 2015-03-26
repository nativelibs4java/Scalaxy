package scalaxy.native

package test

import org.junit._
import org.junit.runner.RunWith
import org.junit.runners.Parameterized.Parameters

import scala.collection.JavaConversions._
import org.bridj.Pointer


class CLError(val code: Int) extends AnyVal


@includes(
  files = Array(
    "haaa",
    "bbb"),
  classes = Array(
    classOf[Test]))
class Test
{
  // c"""
  //   #include <aaa>
  //   #include <bbb>
  //   #include <scalaxy.native.test.Test>
  // """

  sealed trait CLEvent_
  type CLEvent = Ptr[CLEvent_]

  // @native def clWait(events: Array[CLEvent]): CLError = c"""
  //   scalaxy::local_array<jlong> pEvents(env, events);
  //   //clWait(pEvents);
  // """

  // def doSomething: String = ???

  // @native def callDoSomething(p: Ptr[Byte]): CLError = c"""
  //   // scalaxy::object<Test> self(instance);
  //   // self.doSomething();

  //   // // this.
  //   // if (true) {
  //   //   int i;
  //   //   throw scalaxy::error("blah");
  //   // }
  // """

  // @native def clRead(p: Ptr[Byte]): CLError = c"""
    
  // """

  // @native def f(n: Int): Double = c"""
  //   double tot = 10;

  //   for (int i = 0; i < n; ++i) {
  //     tot += i;
  //   }

  //   return tot;
  // """

  // @native def copy(from: Pointer[Int], to: Pointer[Int], n: Int): Unit = c"""
  //   for (int i = 0; i < n; ++i) {
  //     to[i] = from[i];
  //   }
  // """
  @native def copy(v: Long, n: Int): Long = c"""
    size_t vv = (size_t)v;
    return vv << n;
  """

  // @Test
  // def test {
  //   val x: Int = c"""
  //     int f() {

  //       return 10;
  //     }
  //   """

  // }
}

object Test {
  library("test")
}
