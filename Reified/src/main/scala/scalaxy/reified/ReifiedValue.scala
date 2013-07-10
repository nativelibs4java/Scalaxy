package scalaxy.reified

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror

import scalaxy.reified.impl.CaptureConversions
import scalaxy.reified.impl.CaptureConversions.Conversion
import scalaxy.reified.impl.CaptureTag
import scalaxy.reified.impl.CurrentMirrorTreeCreator
import scalaxy.reified.impl.Utils.typeCheck

class ReifiedValue[A] private[reified] (
    val value: A,
    private[reified] val taggedExpr: universe.Expr[A],
    val captures: Seq[(AnyRef, universe.Type)]) {

  def expr(conversion: Conversion = CaptureConversions.DEFAULT): universe.Expr[A] = {
    import universe._
    mapTaggedExpr(new Transformer {
      override def transform(tree: universe.Tree): Tree = {
        tree match {
          case CaptureTag(_, _, captureIndex) =>
            val (capturedValue, valueType) = captures(captureIndex)
            // TODO: add identity set to detect conversion cycles
            // (useless for immutable types, though)
            val converter: Conversion = conversion.orElse({
              case _ =>
                sys.error(s"This type of captured value is not supported: $capturedValue")
            })
            val converted = converter((capturedValue, valueType, converter))
            //typeCheck(converted)
            converted
          case _ =>
            super.transform(tree)
        }
      }
    })
  }
  
  override def toString = {
    if (value == null)
      "null"
    else
      value.toString
  }
  
  private[reified] def mapTaggedExpr(transformer: universe.Transformer): universe.Expr[A] = {
    universe.Expr[A](
      currentMirror,
      CurrentMirrorTreeCreator(transformer.transform(taggedExpr.tree)))
  }

  private[reified] def taggedExprWithOffsetCaptureIndices(offset: Int): universe.Expr[A] = {
    import universe._
    mapTaggedExpr(new Transformer {
      override def transform(tree: universe.Tree): Tree = {
        tree match {
          case CaptureTag(tpe, ref, captureIndex) =>
            CaptureTag.construct(tpe, ref, captureIndex + offset)
          case _ =>
            super.transform(tree)
        }
      }
    })
  }
}

object ReifiedValue {
  def apply[A](value: A, taggedExpr: universe.Expr[A], captures: Seq[(AnyRef, universe.Type)]): ReifiedValue[A] = {
    if (value.isInstanceOf[Function1[_, _]] &&
        taggedExpr.tree.isInstanceOf[scala.reflect.api.Trees#Function]) {
      new ReifiedFunction[Any, Any](
        value.asInstanceOf[Function[Any, Any]], 
        taggedExpr.asInstanceOf[universe.Expr[Function[Any, Any]]], 
        captures).asInstanceOf[ReifiedValue[A]]
    } else {
      new ReifiedValue[A](value, taggedExpr, captures)
    }
  }
}

