package scalaxy.streams

package test

import org.junit._
import org.junit.runner.RunWith
import org.junit.runners.Parameterized.Parameters

import scala.collection.JavaConversions._

@RunWith(classOf[Parallelized])
class MacroIntegrationTest(name: String, source: String, expectedMessages: CompilerMessages)
    extends StreamComponentsTestBase with StreamTransforms {

  import MacroIntegrationTest.strategy
  @Test def test = testMessages(source, expectedMessages)
}

object MacroIntegrationTest {
  scalaxy.streams.impl.verbose = true
  scalaxy.streams.impl.veryVerbose = false

  implicit def strategy = scalaxy.streams.strategy.default

  @Parameters(name = "{0}")
  def data: java.util.Collection[Array[AnyRef]] =
    IntegrationTests.data.map(t =>
      Array[AnyRef](t.name, t.source, t.expectedMessages))
}
