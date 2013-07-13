package scalaxy.reified.internal

import scala.reflect.runtime.universe._
import scala.reflect.runtime.currentMirror

/**
 * Object that enables tagging of values captured by [[scalaxy.reified.ReifiedValue]]'s ASTs.
 * Provides creation and extraction of capture tagging calls.
 */
object CaptureTag {

  /**
   * Used to tag captures of external constants or reified values / functions in ASTs.
   * This is not meant to be called at runtime, it just exists to put a matchable call in the AST.
   *  @param ref original reference to the captured value, kept in the AST for correct typing by the toolbox at runtime.
   *  @param captureIndex index in the captures array of the runtime value of the captured reference
   */
  def apply[T](ref: T, captureIndex: Int): T = ???

  /**
   * Construct the AST for the capture tagging call that corresponds to these params.
   * @param tpe static type of the captured value
   * @param reference original reference that was captured
   * @param captureIndex index of the runtime value of the capture in [[scalaxy.reified.ReifiedValue.capturedTerms]]
   */
  def construct(tpe: Type, reference: Tree, captureIndex: Int): Tree = {
    Apply(
      TypeApply(Ident(captureSymbol), List(TypeTree(tpe))),
      List(
        reference,
        Literal(
          Constant(captureIndex))))
  }

  /**
   * Deconstructor for [[scalaxy.reified.internal.CaptureTag.apply]] tagging calls in ASTs.
   *  @return if a [[scalaxy.reified.internal.CaptureTag.apply]] tagging call was matched, return some tuple made of the static type of the captured value, the original reference that was captured, and the index of the runtime value of the capture in [[scalaxy.reified.ReifiedValue#capturedTerms]]. Otherwise, return None.
   */
  def unapply(tree: Tree): Option[(Type, Tree, Int)] = {
    tree match {
      case Apply(
        TypeApply(f, List(tpt)),
        List(
          value,
          Literal(
            Constant(captureIndex: Int)))) if f.symbol == captureSymbol =>
        Some((tpt.tpe, value, captureIndex))
      case _ =>
        None
    }
  }

  /**
   * Symbol of CaptureTag.apply method
   */
  private lazy val captureSymbol = {
    val captureModule = currentMirror.staticModule("scalaxy.reified.internal.CaptureTag")
    captureModule.moduleClass.typeSignature.member("apply": TermName)
  }
}
