package com.acme.integration

import scalaxy.debug._
import scalaxy.loops._
import scalaxy.beans._
import scalaxy.reified._

import scala.language.postfixOps

import org.junit._
import org.junit.Assert._

class IntegrationTest {
  @Test
  def testReifiedWithDebug {
    import scalaxy.reified._
    scalaxy.debug.assert(10 == q"10".compile()())
  }

  @Test
  def testLoops {
    var tot = 0
    for (i <- 0 to 10 optimized;
         j <- 0 to 2 optimized;
         if i != j) {
      tot += i * 10 + j
    }
    assertEquals(1650, tot)
  }
}
