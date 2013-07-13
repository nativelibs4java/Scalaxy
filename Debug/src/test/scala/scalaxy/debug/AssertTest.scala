package scalaxy.debug.test

import org.junit._
import org.junit.Assert._

import scalaxy.debug._

class AssertTest {
  def assertFails(kind: String, v: => Unit, msg: => String) {
    try {
      v
      assertTrue("Expected AssertError!", false)
    } catch {
      case e @ (_: AssertionError | _: IllegalArgumentException) =>
        assertEquals(s"$kind failed: $msg", e.getMessage)
    }
  }
  
  @Test
  def testConstants = {
    assert(true)
    assume(true)
    require(true)
    assertFails("assertion",    assert(false), "Always false!")
    assertFails("assumption",   assume(false), "Always false!")
    assertFails("requirement", require(false), "Always false!")
      
    val no = false
    assertFails("assertion",    assert(no), "no")
    assertFails("assumption",   assume(no), "no")
    assertFails("requirement", require(no), "no")
      
    val niet = false
    assertFails("assertion",    assert(no || niet), "no.||(niet)")
    assertFails("assumption",   assume(no || niet), "no.||(niet)")
    assertFails("requirement", require(no || niet), "no.||(niet)")
  }
  
  @Test
  def testEqualValues {
    val a = 10
    val b = 12
    assertFails("assertion",    assert(a == b), "a == b (10 != 12)")
    assertFails("assumption",   assume(a == b), "a == b (10 != 12)")
    assertFails("requirement", require(a == b), "a == b (10 != 12)")
  }
  
  @Test
  def testDifferentValues {
    val a = 10
    val aa = 10
    assertFails("assertion",    assert(a != aa), "a != aa (== 10)")
    assertFails("assumption",   assume(a != aa), "a != aa (== 10)")
    assertFails("requirement", require(a != aa), "a != aa (== 10)")
    
    assertFails("assertion",    assert(a != 10), "a != 10")
    assertFails("assumption",   assume(a != 10), "a != 10")
    assertFails("requirement", require(a != 10), "a != 10")
    
    assertFails("assertion",    assert(10 != a), "10 != a")
    assertFails("assumption",   assume(10 != a), "10 != a")
    assertFails("requirement", require(10 != a), "10 != a")
  }
}
