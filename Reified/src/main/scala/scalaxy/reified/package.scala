package scalaxy

import scala.language.experimental.macros

import scala.reflect._
import scala.reflect.macros.Context
import scala.reflect.runtime
import scala.reflect.runtime.universe

import scalaxy.reified.impl.Reification

package object reified
{
  def reify[A, B](f: A => B): ReifiedFunction[A, B] = macro reified.impl.reifyFunction[A, B]
  
  def reify[A](v: A): ReifiedValue[A] = macro reified.impl.reifyValue[A]
  
  def composeValues[A](values: Seq[_ <: ReifiedValue[_]])(compositor: Seq[universe.Expr[_]] => (A, universe.Expr[A])): ReifiedValue[A] = {
    val offsets = values.scanLeft(0)({ 
      case (cumulativeOffset, value) =>
        cumulativeOffset + value.reification.captures.size
    }).dropRight(1)
    val taggedExprs = values.zip(offsets).map({ case (value, offset) =>
      value.reification.taggedExprWithOffsetCaptureIndices(offset)
    })
    
    val (valueResult, exprResult) = compositor(taggedExprs)
    ReifiedValue[A](
      valueResult,
      new Reification[A](
        exprResult,
        values.flatMap(_.reification.captures)
      )
    )
  }
}

