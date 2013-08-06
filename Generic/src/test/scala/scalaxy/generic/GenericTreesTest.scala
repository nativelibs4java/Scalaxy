package scalaxy.generic.test

import scalaxy.generic._
import scalaxy.generic.trees._

import org.junit._
import org.junit.Assert._

// import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.TypeTag
// import scala.reflect.runtime.currentMirror

import scalaxy.generic.trees._
import scala.language.implicitConversions
// import scala.language.dynamics

class GenericTreesTest {

  @Test
  def testTreeRewrite {
    import scala.reflect.runtime.universe._
    import scala.reflect.runtime.currentMirror
    import scala.tools.reflect.ToolBox

    val tb = currentMirror.mkToolBox()

    def genericOpTree[A: Generic: TypeTag]: Tree = {
      reify({
        var a = one[A]
        a = a + number[A](10)
        a = a * number[A](2)
        a = a / number[A](3)
        a.toDouble
      }).tree
    }
    val genericDoubleTree = genericOpTree[Double]
    val doubleTree = reify({
      var a = 1.0
      a = (a + 10.0).asInstanceOf[Double]
      a = (a * 2.0).asInstanceOf[Double]
      a = (a / 3.0).asInstanceOf[Double]
      a.toDouble.asInstanceOf[Double]
    }).tree
    assertEquals(
      tb.typeCheck(doubleTree).toString,
      simplifyGenericTree(tb.typeCheck(genericDoubleTree)).toString
        .replaceAll("\\$times", "*")
        .replaceAll("\\$plus", "+")
        .replaceAll("\\$minus", "-")
        .replaceAll("\\$div", "/"))
  }
}
