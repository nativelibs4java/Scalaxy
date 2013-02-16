package scalaxy.debug.test

import org.junit._
import org.junit.Assert._

import scalaxy.debug.assert

class AssertTest {
  def assertAssertFails(v: => Unit, msg: => String) {
    try {
      v
      assertTrue("Expected AssertError!", false)
    } catch {
      case e: AssertionError =>
        assertEquals(s"assertion failed: $msg", e.getMessage)
    }
  }
  
  @Test
  def testConstants = {
    assert(true)
    assertAssertFails(
      assert(false),
      "Always false!")
      
    val no = false
    assertAssertFails(
      assert(no),
      "no == false")
      
    val niet = false
    assertAssertFails(
      assert(no || niet),
      "no.||(niet) == false")
  }
  
  @Test
  def testEqualValues {
    val a = 10
    val b = 12
    assertAssertFails(
      assert(a == b),
      "a != b (10 != 12)")
  }
  
  @Test
  def testDifferentValues {
    val a = 10
    val aa = 10
    assertAssertFails(
      assert(a != aa),
      "a == aa (== 10)")
    assertAssertFails(
      assert(a != 10),
      "a == 10")
    assertAssertFails(
      assert(10 != a),
      "10 == a")
  }
}
