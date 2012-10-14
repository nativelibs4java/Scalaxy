package scalaxy; package test

import plugin._

import org.junit._
import Assert._

//@Ignore
class NumericsTest extends BaseTestUtils {

  override def pluginDef = new ScalaxyPluginDefLike {
    override def matchActionHolders = Seq(compilets.Numerics)
  }
  
  override def commonImports = """
    import math.Numeric.Implicits._
    import Ordering.Implicits._
  """
  
  def testBinOp(op: String, name: String) {
    ensurePluginCompilesSnippetsToSameByteCode(
      "def " + name + "[T : Numeric](a: T, b: T) = " +
        "a " + op + " b",
      "def " + name + "[T : Numeric](a: T, b: T) = " +
        "implicitly[Numeric[T]]." + name + "(a, b)"
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
