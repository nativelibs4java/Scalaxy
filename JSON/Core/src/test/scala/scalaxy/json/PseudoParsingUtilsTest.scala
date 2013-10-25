package scalaxy.json.test

import org.junit._
import Assert._

class PseudoParsingUtilsTest {
  import scalaxy.json.base.PseudoParsingUtils._
  import scalaxy.json.base.JSONPseudoParsingUtils._

  val dotsRx = """\.\.\.""".r
  def findDots(s: String) = dotsRx.findAllMatchOutsideJSONCommentsAndStringsIn(s).map(_.start).toList

  @Test
  def dots {
    assertEquals(
      List(0, 31, 38),
      findDots("""... /* blaa... *... */ "ah..." ... oh ... '...'"""))
  }

}
