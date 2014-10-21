package scalaxy.streams

package test

import org.junit._
import org.junit.runner.RunWith
import org.junit.runners.Parameterized.Parameters

import scala.collection.JavaConversions._

import IntegrationTests.{streamMsg, potentialSideEffectMsgs}

/** This is just a testbed for "fast" manual tests */
class CustomTest extends StreamComponentsTestBase with StreamTransforms {
  scalaxy.streams.impl.verbose = true
  scalaxy.streams.impl.veryVerbose = false

  val fnRx = raw".*scala\.Function0\.apply.*"

  @Ignore
  @Test
  def testPrints {
    val src = "(0 to 2).map(i => () => i).map(_()).takeWhile(_ < 1)"

    { import scalaxy.streams.strategy.safer
      testMessages(src, streamMsg("Range.map.map -> IndexedSeq")) }
  }
}
