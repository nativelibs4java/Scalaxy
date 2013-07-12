package scalaxy.reified

import scala.reflect.runtime.universe._

import scalaxy.reified.impl.CaptureTag
import scalaxy.reified.impl.Utils._

/**
 * Reified value wrapper.
 */
private[reified] trait HasReifiedValue[A] {
  private[reified] def reifiedValue: ReifiedValue[A]
  def valueTag: TypeTag[A]
  override def toString = s"${getClass.getSimpleName}(${reifiedValue.value}, ${reifiedValue.taggedExpr.tree}, ${reifiedValue.capturedTerms})"
}

/**
 * Reified value which can be created by {@link scalaxy.reified.reify}.
 */
final case class ReifiedValue[A: TypeTag] private[reified] (
  val value: A,
  val taggedExpr: Expr[A],
  val capturedTerms: Seq[(AnyRef, Type)])
    extends HasReifiedValue[A] {

  override def reifiedValue = this
  override def valueTag = typeTag[A]

  def capturedValues: Seq[AnyRef] = capturedTerms.map(_._1)

  def flatten(capturesOffset: Int = 0): ReifiedValue[A] = {
    val flatCapturedTerms = collection.mutable.ArrayBuffer[(AnyRef, Type)]()
    flatCapturedTerms ++= capturedTerms

    val mappedExpr = mapTaggedExpr(new Transformer {
      override def transform(tree: Tree): Tree = {
        tree match {
          case CaptureTag(tpe, ref, captureIndex) =>
            capturedTerms(captureIndex) match {
              case (value: ReifiedValue[_], _) =>
                val sub = value.flatten(capturesOffset + capturedTerms.size)
                val subTree = sub.taggedExpr.tree

                flatCapturedTerms ++= sub.capturedTerms
                subTree /*
                if (value.taggedExpr.tree eq subTree)
                  subTree.duplicate
                else
                  subTree*/
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
      flatCapturedTerms.toList)
  }

  /**
   * Get the AST of this reified value, using the specified conversion function for any
   * value that was captured by the expression.
   */
  def expr(conversion: CaptureConversions.Conversion = CaptureConversions.DEFAULT): Expr[A] = {
    stableExpr(conversion)
    //optimizedExpr(conversion)
  }

  private def stableExpr(conversion: CaptureConversions.Conversion = CaptureConversions.DEFAULT): Expr[A] = {
    mapTaggedExpr(new Transformer {
      override def transform(tree: Tree): Tree = {
        tree match {
          case CaptureTag(_, _, captureIndex) =>
            val (capturedValue, valueType) = capturedTerms(captureIndex)
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

  private def optimizedExpr(conversion: CaptureConversions.Conversion = CaptureConversions.DEFAULT): Expr[A] = {
    val flatCaptures = new collection.mutable.ArrayBuffer[(AnyRef, Type)]()
    flatCaptures ++= capturedTerms
    val captureValueTrees = new collection.mutable.HashMap[Int, Tree]()
    def captureRefName(captureIndex: Int): TermName = "scalaxy$capture$" + captureIndex

    // TODO predeclare the captures in a block that returns the function
    val function = (new Transformer {
      override def transform(tree: Tree): Tree = {
        tree match {
          case CaptureTag(_, _, captureIndex) =>
            val (capturedValue, valueType) = capturedTerms(captureIndex)
            capturedValue match {
              case hr: HasReifiedValue[_] =>
                val flatRef = hr.reifiedValue.flatten(flatCaptures.size)
                flatCaptures ++= flatRef.capturedTerms
                // TODO maybe no need to duplicate if tree was unchanged (when no capture)
                super.transform(hr.reifiedValue.taggedExpr.tree) //.duplicate)
              case _ =>
                val converter: CaptureConversions.Conversion = conversion.orElse({
                  case _ =>
                    sys.error(s"This type of captured value is not supported: $capturedValue")
                })
                captureValueTrees(captureIndex) = converter((capturedValue, valueType, converter))
                Ident(captureRefName(captureIndex))
            }
          case _ =>
            super.transform(tree)
        }
      }
    }).transform(taggedExpr.tree)

    val res = newExpr[A](
      if (flatCaptures.isEmpty)
        function
      else
        Block(
          (
            for {
              ((_, tpe), captureIndex) <- flatCaptures.zipWithIndex
              tree = captureValueTrees(captureIndex)
            } yield {
              ValDef(Modifiers(Flag.LOCAL), captureRefName(captureIndex), TypeTree(if (tpe == null) NoType else tpe), tree)
            }
          ).toList,
          function
        )
    )
    println(s"res = $res")
    res //typeCheck(res)
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

