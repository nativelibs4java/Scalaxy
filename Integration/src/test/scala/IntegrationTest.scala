package com.acme.integration

import scalaxy.debug._
import scalaxy.loops._
import scalaxy.beans._
import scalaxy.reified._

import org.junit._
import org.junit.Assert._

class IntegrationTest {
  @Test
  def testReifiedWithDebug {
    import scalaxy.reified._
    scalaxy.debug.assert(10 == reify(10).compile()())
  }
}
