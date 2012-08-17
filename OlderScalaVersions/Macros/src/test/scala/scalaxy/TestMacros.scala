package scalaxy; package test

//import scalaxy.macros.{ Replacement }
import macros._
    
import org.junit._
import Assert._
import scalaxy.macros.fail

import scala.reflect._
import mirror._
    
class TestMacros 
{
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
