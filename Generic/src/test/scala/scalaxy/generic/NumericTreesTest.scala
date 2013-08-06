package scalaxy.generic.test

import org.junit._
import org.junit.Assert._

// import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.TypeTag
// import scala.reflect.runtime.currentMirror

import scalaxy.generic.trees._
import scala.language.implicitConversions
// import scala.language.dynamics

class NumericTreesTest {

  @Test
  def testTreeRewrite {
    import scala.reflect.runtime.universe._
    import scala.reflect.runtime.currentMirror
    import scala.tools.reflect.ToolBox

    val tb = currentMirror.mkToolBox()

    def numericOpTree[A: Numeric: TypeTag]: Tree = {
      import Numeric.Implicits._
      reify({
        var a = implicitly[Numeric[A]].one
        a = a + implicitly[Numeric[A]].fromInt(10)
        a = a * implicitly[Numeric[A]].fromInt(2)
        // a = a / implicitly[Numeric[A]].fromInt(3)
        a.toDouble
      }).tree
    }
    val numericDoubleTree = numericOpTree[Double]
    val doubleTree = reify({
      var a = 1.0
      a = a + (10.0)
      a = a * (2.0)
      // a = (a / 3.0)
      a.toDouble
    }).tree
    assertEquals(tb.typeCheck(doubleTree).toString,
      simplifyGenericTree(tb.typeCheck(numericDoubleTree)).toString
        .replaceAll("\\$times", "*")
        .replaceAll("\\$plus", "+")
        .replaceAll("\\$minus", "-")
        .replaceAll("\\$div", "-"))
  }
}
