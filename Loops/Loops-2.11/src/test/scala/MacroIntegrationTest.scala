package scalaxy.loops

package test

import org.junit._
import Assert._

class MacroIntegrationTest extends StreamComponentsTestBase with StreamOps {
  import toolbox.u._

  @Test def testArrayMapFilter {
    assertMacroCompilesToSameValue("Array(1, 2, 3).map(_ * 2).filter(_ < 3)")
  }

  @Test def testRangeMapFilter {
    assertMacroCompilesToSameValue("(1 to 10).map(_ * 2).filter(_ < 3)")
  }
}
