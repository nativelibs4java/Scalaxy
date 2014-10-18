package scalaxy.streams

package test

import org.junit._
import org.junit.runner.RunWith
import org.junit.runners.Parameterized.Parameters

import scala.collection.JavaConversions._

import IntegrationTests.streamMsg

class CustomTest extends StreamComponentsTestBase with StreamTransforms {
  scalaxy.streams.impl.verbose = true

  @Test def test = testMessages(
    """
      import scalaxy.streams.optimization.aggressive
      for (v <- Array(1, 2, 3)) yield v + 1
    """,
    streamMsg("Array.map -> Array")
  )
}
