package scalaxy.reified

import scala.reflect.runtime.universe._

import scalaxy.reified.impl.CaptureTag
import scalaxy.reified.impl.Utils.newExpr

final case class ReifiedValue[A] private[reified] (
    val value: A,
    val taggedExpr: Expr[A],
    val captures: Seq[(AnyRef, Type)]) {

  def capturedValues: Seq[AnyRef] = captures.map(_._1)

  def flattenCaptures(capturesOffset: Int): ReifiedValue[A] = {
    val flatCaptures = collection.mutable.ArrayBuffer[(AnyRef, Type)]()
    flatCaptures ++= captures

    val mappedExpr = mapTaggedExpr(new Transformer {
      override def transform(tree: Tree): Tree = {
        tree match {
          case CaptureTag(tpe, ref, captureIndex) =>
            captures(captureIndex) match {
              case (value: ReifiedValue[_], _) =>
                val sub = value.flattenCaptures(capturesOffset + captures.size)
                val subTree = sub.taggedExpr.tree

                flatCaptures ++= sub.captures
                if (value.taggedExpr.tree eq subTree)
                  subTree.duplicate
                else
                  subTree
              case _ =>
                CaptureTag.construct(tpe, ref, captureIndex + capturesOffset)
            }
          case _ =>
            super.transform(tree)
        }
      }
    })
    new ReifiedValue[A](
      value,
      mappedExpr,
      flatCaptures.toSeq)
  }

  def expr(conversion: CaptureConversions.Conversion = CaptureConversions.DEFAULT): Expr[A] = {
    mapTaggedExpr(new Transformer {
      override def transform(tree: Tree): Tree = {
        tree match {
          case CaptureTag(_, _, captureIndex) =>
            val (capturedValue, valueType) = captures(captureIndex)
            val converter: CaptureConversions.Conversion = conversion.orElse({
              case _ =>
                sys.error(s"This type of captured value is not supported: $capturedValue")
            })
            converter((capturedValue, valueType, converter))
          case _ =>
            super.transform(tree)
        }
      }
    })
  }

  private[reified] def mapTaggedExpr(transformer: Transformer): Expr[A] = {
    newExpr[A](transformer.transform(taggedExpr.tree))
  }

  private[reified] def taggedExprWithOffsetCaptureIndices(offset: Int): Expr[A] = {
    mapTaggedExpr(new Transformer {
      override def transform(tree: Tree): Tree = {
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

