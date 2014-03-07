package scalaxy.loops

package test

import org.junit._
import org.junit.Assert._
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(classOf[Parameterized])
class MacroIntegrationTest(source: String) extends StreamComponentsTestBase with StreamOps {
  @Test def test {
    assertMacroCompilesToSameValue(source)
  }
}

object MacroIntegrationTest extends StreamComponentsTestBase with StreamOps {
  import toolbox.u._

  @Parameters(name = "{0}")
  def data: java.util.Collection[Array[AnyRef]] = {
    import collection.JavaConversions._
    List(
      "Array(1, 2, 3).map(_ * 2).filter(_ < 3)",
      "(2 to 10).map(_ * 2).filter(_ < 3)",
      "(2 until 10 by 2).map(_ * 2)",
      "(20 to 7 by -3).map(_ * 2).filter(_ < 3)",
      "Array(1, 2, 3).map(_ * 2).map(_ < 3)"
    ).map(Array(_: AnyRef))
  }
}
