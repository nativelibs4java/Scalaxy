package scalaxy.reified

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror

import scalaxy.reified.impl.Capture
import scalaxy.reified.impl.CurrentMirrorTreeCreator
import scalaxy.reified.impl.TypeChecks.typeCheck

class ReifiedValue[A](
    val value: A,
    private[reified] val rawExpr: universe.Expr[A],
    val captures: Seq[AnyRef]) {

  val expr: universe.Expr[A] = {
    import universe._
    val transformer = new Transformer {
      override def transform(tree: universe.Tree): Tree = {
        tree match {
          case Capture(captureIndex) =>
            val capturedValue = captures(captureIndex)
            capturedValue match {
              case v: Integer =>
                Literal(Constant(v))
              case r: ReifiedValue[_] =>
                r.expr.tree.duplicate
              case _ =>
                sys.error(s"This type of captured value is not supported: $capturedValue")
            }
          case _ =>
            super.transform(tree)
        }
      }
    }
    
    val rawChecked = typeCheck(rawExpr.tree)
    Expr[A](
      currentMirror,
      CurrentMirrorTreeCreator(transformer.transform(rawChecked)))
  }
}

