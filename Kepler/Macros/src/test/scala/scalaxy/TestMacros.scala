package scalaxy; package test

//import scalaxy.macros.{ Replacement }
import macros._
    
import org.junit._
import Assert._
import scalaxy.macros.fail

import scala.reflect.mirror._
    
class TestMacros 
{
  /*
  @Test
  def testReplaceTypeVar {
    def rep[T](v: T) = replace(v.toString, "?")
    
    println("rep(12) = " + rep(12))
  }
  */
  
  import math.Numeric.Implicits._
  import Ordering.Implicits._
  
  def plus[T : TypeTag](a: T, b: T)(implicit n: Numeric[T]) = replace(
    a + b, // Numeric.Implicits.infixNumericOps[T](a)(n).+(b)
    n.plus(a, b)
  )
  
  @Test
  def testReplace {
    replace(1, 1) match { 
      case Replacement(
        Expr(Literal(Constant(1))),
        Expr(Literal(Constant(1)))
      ) => 
    }
  }
  @Test
  def testFail {
    fail("hehe") { 1 } match { 
      case MatchError(
        Expr(Literal(Constant(1))),
        "hehe"
      ) => 
    }
  }
  @Test
  def testWarn {
    warn("hehe") { 1 } match { 
      case MatchWarning(
        Expr(Literal(Constant(1))),
        "hehe"
      ) => 
    }
  }
  @Test
  def testWarning {
    warning[Unit]("hehe") match { 
      case Warning("hehe") => 
    }
  }
  @Test
  def testError {
    error[Unit]("hehe") match { 
      case Error("hehe") => 
    }
  }
  @Test
  def testReplacement {
    replacement(1) match { 
      case ReplaceBy(Expr(Literal(Constant(1)))) =>
      case v =>
        assertTrue("got " + v, false)
    }
  }
}
