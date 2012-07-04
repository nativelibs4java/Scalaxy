package scalaxy.inlinable

import org.junit._
import Assert._

class InlinableTest {
  
  val n = 10
  
  @Test
  def testTo {
    var tot = 0; for (i <- 0 to_ n) tot += 1
    assertEquals(n + 1, tot)
  }
  
  @Test
  def testUntil {
    var tot = 0; for (i <- 0 until_ n) tot += 1
    assertEquals(n, tot)
  }
  
  @Test
  def testToConstantStep {
    {
      var tot = 0; for (i <- 0 to_ n by 2) tot += 1
      assertEquals(n / 2 + 1, tot)
    }
    {
      var tot = 0; for (i <- n to_ 0 by -2) tot += 1
      assertEquals(n / 2 + 1, tot)
    }
  }
  
  @Test
  def testUntilConstantStep {
    {
      var tot = 0; for (i <- 0 until_ n by 2) tot += 1
      assertEquals(n / 2, tot)
    }
    {
      var tot = 0; for (i <- n until_ 0 by -2) tot += 1
      assertEquals(n / 2, tot)
    }
  }
  
  
  val posStep = 2
  val negStep = -2
  
  @Test
  def testToVariableStep {
    {
      var tot = 0; for (i <- 0 to_ n by posStep) tot += 1
      assertEquals(n / 2 + 1, tot)
    }
    {
      var tot = 0; for (i <- n to_ 0 by negStep) tot += 1
      assertEquals(n / 2 + 1, tot)
    }
  }
  
  @Test
  def testUntilVariableStep {
    {
      var tot = 0; for (i <- 0 until_ n by 2) tot += 1
      assertEquals(n / 2, tot)
    }
    {
      var tot = 0; for (i <- n until_ 0 by -2) tot += 1
      assertEquals(n / 2, tot)
    }
  }
  
  @Test
  def testConstantEmpty {
    var tot = 0; 
    
    for (i <- 0 to_ n by -2) tot += 1
    assertEquals(0, tot)
    
    for (i <- n to_ 0) tot += 1
    assertEquals(0, tot)
    
    for (i <- n to_ 0 by 2) tot += 1
    assertEquals(0, tot)
    
    for (i <- 0 until_ n by -2) tot += 1
    assertEquals(0, tot)
    
    for (i <- n until_ 0 by 2) tot += 1
    assertEquals(0, tot)
  }
  
  @Test
  def testAlias {
    import collection.mutable.ArrayBuffer
    
    val Seq(is, js) = (0 until 2).map(_ => new ArrayBuffer[Int])
    
    for (i <- 0 to_ 3) {
      val j: Int = i
      js += j
      for (i <- 2 until_ 0 by -1)
        is += i
    }
    
    assertEquals(List(0, 1, 2, 3), js.toList)
    assertEquals(List(2, 1, 2, 1, 2, 1, 2, 1), is.toList)
  }
}
