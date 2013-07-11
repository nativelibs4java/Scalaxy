package scalaxy.reified.impl

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror

import scalaxy.reified.CaptureConversions.Conversion
import scalaxy.reified.impl.CaptureTag
import scalaxy.reified.impl.CurrentMirrorTreeCreator
import scalaxy.reified.impl.Utils.typeCheck

class Reification[A](
    val taggedExpr: universe.Expr[A],
    val captures: Seq[(AnyRef, universe.Type)]) {

  //def flattenCaptures(capturesOffset: Int): (universe.Expr[A], 
  def expr(conversion: Conversion): universe.Expr[A] = {
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
    s"""${getClass.getSimpleName}($taggedExpr, Captures(${captures.mkString(", ")}))"""
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
