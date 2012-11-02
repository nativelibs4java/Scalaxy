package scalaxy.test

import org.junit._

class NumericsTest extends BaseTestUtils
{
  override def compilets = Seq(scalaxy.compilets.Numerics)

  override def commonImports = """
    import math.Numeric.Implicits._
    import Ordering.Implicits._
  """

  //def testBinOp(op: String, name: String) {
  //  ensurePluginCompilesSnippetsToSameByteCode(
  //    "def " + name + "[T : Numeric](a: T, b: T) = " +
  //      "a " + op + " b",
  //    "def " + name + "[T : Numeric](a: T, b: T) = " +
  //      "implicitly[Numeric[T]]." + name + "(a, b)"
  //  )
  //}
  def testBinOp(op: String, name: String) {
    ensurePluginCompilesSnippetsToSameByteCode(
      "def " + name + "[T](a: T, b: T)(implicit n: Numeric[T]) = " +
        "a " + op + " b",
      "def " + name + "[T](a: T, b: T)(implicit n: Numeric[T]) = " +
        "n." + name + "(a, b)"
    )
  }

  @Test
  def plus = testBinOp("+", "plus")
  @Test
  def minus = testBinOp("-", "minus")
  @Test
  def times = testBinOp("*", "times")
  @Test
  def gt = testBinOp(">", "gt")
  @Test
  def lt = testBinOp("<", "lt")
  @Test
  def gteq = testBinOp(">=", "gteq")
  @Test
  def lteq = testBinOp("<=", "lteq")
}
