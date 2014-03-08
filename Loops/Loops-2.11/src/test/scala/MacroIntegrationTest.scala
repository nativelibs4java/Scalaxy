package scalaxy.loops

package test

import org.junit._
import org.junit.Assert._
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

import scala.collection.JavaConversions._

@RunWith(classOf[Parameterized])
class MacroIntegrationTest(source: String) extends StreamComponentsTestBase with StreamOps {
  @Test def test = assertMacroCompilesToSameValue(source)
}

object MacroIntegrationTest extends StreamComponentsTestBase with StreamOps {
  @Parameters(name = "{0}") def data: java.util.Collection[Array[AnyRef]] = List[AnyRef](
    "Array(1, 2, 3).map(_ * 2).filter(_ < 3)",
    "(2 to 10).map(_ * 2).filter(_ < 3)",
    "(2 until 10 by 2).map(_ * 2)",
    "(20 to 7 by -3).map(_ * 2).filter(_ < 3)",
    "Array(1, 2, 3).map(_ * 2).map(_ < 3)",
    "(10 to 20).map(i => () => i).map(_())",
    "(10 to 20).map(_ + 1).map(i => () => i).map(_())",
    "(10 to 20).map(_ * 10).map(i => () => i).reverse.map(_())",
    "for ((v, i) <- (20 until 0 by -2).zipWithIndex) yield (v + i)",
    "Array((1, 2), (3, 4)).map(_ match { case p @ (i, j) => (i * 10, j / 10.0) }).map({ case (k, l) => k + l })"
  ).map(Array(_))
}
