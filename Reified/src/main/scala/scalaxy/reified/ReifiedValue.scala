package scalaxy.reified

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror

import scalaxy.reified.impl.CaptureTag
import scalaxy.reified.impl.CurrentMirrorTreeCreator

object ReifiedValue {
  def apply[A](value: A, taggedExpr: universe.Expr[A], captures: Seq[AnyRef]): ReifiedValue[A] = {
    if (value.isInstanceOf[Function1[_, _]]) {
      new ReifiedFunction[Any, Any](
        value.asInstanceOf[Function[Any, Any]], 
        taggedExpr.asInstanceOf[universe.Expr[Function[Any, Any]]], 
        captures).asInstanceOf[ReifiedValue[A]]
    } else {
      new ReifiedValue[A](value, taggedExpr, captures)
    }
  }
}

class ReifiedValue[A](
    val value: A,
    private[reified] val taggedExpr: universe.Expr[A],
    val captures: Seq[AnyRef]) {

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

  lazy val expr: universe.Expr[A] = {
    import universe._
    mapTaggedExpr(new Transformer {
      override def transform(tree: universe.Tree): Tree = {
        tree match {
          case CaptureTag(_, _, captureIndex) =>
            val capturedValue = captures(captureIndex)
            capturedValue match {
              case (_: Number) | (_: String) | (_: java.lang.Character) =>
                Literal(Constant(capturedValue))
              // TODO: convert immutable array, seq, list, set, map
              //case _: Array[_] =>
              //  universe.reify(Array[AnyRef]
              case r: ReifiedValue[_] =>
                r.expr.tree.duplicate
              case _ =>
                sys.error(s"This type of captured value is not supported: $capturedValue")
            }
          case _ =>
            super.transform(tree)
        }
      }
    })
  }
}

