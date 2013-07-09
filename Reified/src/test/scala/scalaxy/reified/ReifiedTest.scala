package scalaxy.debug.test

import org.junit._
import org.junit.Assert._

import scalaxy.reified._

class ReifiedTest {
  
  @Test
  def testValue = {
    val x = 10
    val r = reify(10 * x)
    assertTrue(r.isInstanceOf[ReifiedValue[_]])
  }
  
  @Test
  def testFunction = {
    val r = reify((x: Int) => x * x)
    assertTrue(r.isInstanceOf[ReifiedFunction[_, _]])
  }
}
