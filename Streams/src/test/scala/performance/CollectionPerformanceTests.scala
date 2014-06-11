package scalaxy.streams
package test

import org.junit._
import org.junit.Assert._

trait ChainedPerformanceTests {
  this: CollectionPerformanceTests =>
  def chain(du: (String, String)) = {
    val (definition, use) = du
    (definition, use + ".filter(v => (v % 2) == 0).map(_ * 2)")
  }
}

trait NoRightTests { //extends CollectionPerformanceTests {
  this: CollectionPerformanceTests =>
  override def simpleScanRight = {}
  override def simpleFoldRight = {}
  override def simpleReduceRight = {}
}
trait NoScalarReductionTests {//extends CollectionPerformanceTests {
  this: CollectionPerformanceTests =>
  override def simpleSum = {}
  override def simpleProduct = {}
  override def simpleMin = {}
  override def simpleMax = {}
}
@Ignore class ListPerformanceTest extends CollectionPerformanceTests with NoRightTests {
  override def col = ("val col: List[Int] = (0 to n).toList", "col")//.filter(v => (v % 2) == 0).map(_ * 2)")
}
@Ignore class ListChainedPerformanceTest extends ListPerformanceTest with ChainedPerformanceTests {
  override def col = chain(super.col)
}
@Ignore class ArrayPerformanceTest extends CollectionPerformanceTests {
  override def col = ("val col = Array.tabulate(n)(i => i)", "col")
  @Test def simpleArrayTabulate =  if (!skip) ensureFasterCodeWithSameResult(null, "Array.tabulate(n)(i => i).toSeq")
}
@Ignore class ArrayChainedPerformanceTest extends ArrayPerformanceTest with ChainedPerformanceTests {
  override def col = chain(super.col)
}
class RangePerformanceTest extends CollectionPerformanceTests with NoRightTests with NoScalarReductionTests {
  override def col = (null: String, "(0 until n)")
  override def simpleToArray = {}
  override def simpleToList = {}
  override def simpleTakeWhile = {}
  override def simpleDropWhile = {}
  override def simpleSum = {}
  override def simpleProduct = {}
  override def simpleMin = {}
  override def simpleMax = {}
}
class RangeChainedPerformanceTest extends CollectionPerformanceTests with ChainedPerformanceTests with NoRightTests with NoScalarReductionTests {
  override def col = chain((null, "(0 until n)"))
}

trait CollectionPerformanceTests extends PerformanceTests {
  val skip = PerformanceTests.skip
  def col: (String, String)

  /**************************
   * Collection conversions *
   **************************/
  @Test def simpleToArray = if (!skip) testToArray(col)
  @Test def simpleToList = if (!skip) testToList(col)
  // @Ignore @Test def simpleToVector = if (!skip) testToVector(col)

  @Test def simpleFilter = testFilter(col)
  @Test def simpleFilterNot = testFilterNot(col)
  @Ignore @Test def simpleCount = testCount(col)
  @Ignore @Test def simpleExists = testExists(col)
  @Ignore @Test def simpleForall = testForall(col)
  @Ignore @Test def simpleTakeWhile = testTakeWhile(col)
  @Ignore @Test def simpleDropWhile = testDropWhile(col)
  @Test def simpleForeach = testForeach(col)
  @Test def simpleMap = testMap(col)
  @Test def simpleSum = testSum(col)
  @Test def simpleProduct = testProduct(col)
  @Ignore @Test def simpleMin = testMin(col)
  @Ignore @Test def simpleMax = testMax(col)
  @Ignore @Test def simpleScanLeft = testScanLeft(col)
  @Ignore @Test def simpleScanRight = testScanRight(col)
  @Ignore @Test def simpleFoldLeft = testFoldLeft(col)
  @Ignore @Test def simpleFoldRight = testFoldRight(col)
  @Ignore @Test def simpleReduceLeft = testReduceLeft(col)
  @Ignore @Test def simpleReduceRight = testReduceRight(col)

}
