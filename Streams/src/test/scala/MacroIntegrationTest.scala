package scalaxy.streams

package test

import org.junit._
import org.junit.runner.RunWith
import org.junit.runners.Parameterized.Parameters

import scala.collection.JavaConversions._

@RunWith(classOf[Parallelized])
class MacroIntegrationTest(
    name: String,
    source: String,
    expectedMessages: CompilerMessages)
{
  import MacroIntegrationTest._

  @Test
  def test = testMessages(source, expectedMessages)(strategy)
}

object MacroIntegrationTest
    extends StreamComponentsTestBase with StreamTransforms {
  scalaxy.streams.impl.verbose = true
  scalaxy.streams.impl.veryVerbose = false
  scalaxy.streams.impl.quietWarnings = true

  implicit def strategy = scalaxy.streams.strategy.foolish

  @Parameters(name = "{0}")
  def data: java.util.Collection[Array[AnyRef]] =
    IntegrationTests.data.map(t =>
      Array[AnyRef](t.name, t.source, t.expectedMessages))
}
