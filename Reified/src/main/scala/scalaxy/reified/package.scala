package scalaxy

import scala.language.experimental.macros

import scala.reflect._
import scala.reflect.macros.Context
import scala.reflect.runtime
import scala.reflect.runtime.universe

package object reified
{
  def reify[A, B](f: A => B): ReifiedFunction[A, B] = macro reified.impl.reifyFunction[A, B]
  
  def reify[A](v: A): ReifiedValue[A] = macro reified.impl.reifyValue[A]
  
  def composeValues[A](values: Seq[_ <: ReifiedValue[_]])(compositor: Seq[universe.Expr[_]] => (A, universe.Expr[A])): ReifiedValue[A] = {
    val offsets = values.scanLeft(0)({ 
      case (cumulativeOffset, value) =>
        cumulativeOffset + value.captures.size
    }).dropRight(1)
    val taggedExprs = values.zip(offsets).map({ case (value, offset) =>
      value.taggedExprWithOffsetCaptureIndices(offset)
    })
    
    val (valueResult, exprResult) = compositor(taggedExprs)
    ReifiedValue[A](
      valueResult,
      exprResult,
      values.flatMap(_.captures)
    )
  }
}

