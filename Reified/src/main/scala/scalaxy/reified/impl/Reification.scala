package scalaxy.reified.impl

import scalaxy.reified.ReifiedValue

import scala.reflect.runtime.universe

import scalaxy.reified.CaptureConversions.Conversion
import scalaxy.reified.impl.Utils.{ typeCheck, newExpr }

// TODO: add identity set to detect conversion cycles (useless for immutable types, though)
trait HasReification[A] {
  def reification: Reification[A]
}

case class Reification[A](
  val taggedExpr: universe.Expr[A],
  val captures: Seq[(AnyRef, universe.Type)]) {

  def capturedValues: Seq[AnyRef] = captures.map(_._1)
  
  def flattenCaptures(capturesOffset: Int): Reification[A] = {
    import universe._
    
    val flatCaptures = collection.mutable.ArrayBuffer[(AnyRef, universe.Type)]()
    flatCaptures ++= captures
    
    val mappedExpr = mapTaggedExpr(new Transformer {
      override def transform(tree: universe.Tree): Tree = {
        tree match {
          case CaptureTag(tpe, ref, captureIndex) =>
            captures(captureIndex) match {
              case (value: HasReification[_], _) =>
                val reification = value.reification
                val Reification(valueExpr, valueCaptures) = {
                  reification.flattenCaptures(capturesOffset + captures.size)
                }
                flatCaptures ++= valueCaptures
                if (reification.taggedExpr.tree eq valueExpr.tree)
                  valueExpr.tree.duplicate
                else
                  valueExpr.tree
              case _ =>
                CaptureTag.construct(tpe, ref, captureIndex + capturesOffset)
            }
          case _ =>
            super.transform(tree)
        }
      }
    })
    new Reification[A](
      mappedExpr,
      flatCaptures.toSeq)
  }
  def expr(conversion: Conversion): universe.Expr[A] = {
    import universe._
    mapTaggedExpr(new Transformer {
      override def transform(tree: universe.Tree): Tree = {
        tree match {
          case CaptureTag(_, _, captureIndex) =>
            val (capturedValue, valueType) = captures(captureIndex)
            val converter: Conversion = conversion.orElse({
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
  
  override def toString = {
    s"""${getClass.getSimpleName}($taggedExpr, Captures(${captures.mkString(", ")}))"""
  }
  
  private[reified] def mapTaggedExpr(transformer: universe.Transformer): universe.Expr[A] = {
    newExpr[A](transformer.transform(taggedExpr.tree))
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
