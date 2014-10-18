package scalaxy.streams

package test

import org.junit._
import org.junit.runner.RunWith
import org.junit.runners.Parameterized.Parameters

import scala.collection.JavaConversions._

import IntegrationTests.streamMsg

class CustomTest extends StreamComponentsTestBase with StreamTransforms {
  scalaxy.streams.impl.verbose = true
  scalaxy.streams.impl.veryVerbose = true

  @Ignore @Test 
  def test = testMessages(
    """
      val n = 10

     // (0 until n).toList
//
      // (0 until n).dropWhile(x => x < n / 2).toSeq
    List(0, 1, 2).map(_ + 1).toSeq

     // (0 until n).filter(v => (v % 2) == 0).map(_ * 2).toArray.toSeq

//      for (v <- Array(1, 2, 3)) yield v + 1
    """,
    streamMsg("Array.map -> Array"),
    strategy = scalaxy.streams.optimization.foolish
  )
}
