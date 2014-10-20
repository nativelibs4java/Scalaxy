package scalaxy.streams

package test

import org.junit._
import org.junit.runner.RunWith
import org.junit.runners.Parameterized.Parameters

import scala.collection.JavaConversions._

@RunWith(classOf[Parallelized])
class PluginCompilationTest(name: String, source: String)
    extends StreamComponentsTestBase with StreamTransforms {
  @Test def test = assertPluginCompilesSnippetFine(source)
}

object PluginCompilationTest {

  implicit def strategy = scalaxy.streams.strategy.default

  @Parameters(name = "{0}")
  def data: java.util.Collection[Array[AnyRef]] =
    IntegrationTests.data.map(t =>
      Array[AnyRef](t.name, t.source))
}
