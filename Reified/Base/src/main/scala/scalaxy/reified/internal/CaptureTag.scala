package scalaxy.reified.internal

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._
import scala.reflect.runtime.currentMirror

import scalaxy.reified.internal.Utils._

/**
 * Object that enables tagging of values captured by {@link scalaxy.reified.ReifiedValue}'s ASTs.
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
   * @param captureIndex index of the runtime value of the capture in {@link scalaxy.reified.ReifiedValue#capturedTerms}
   */
  def construct(tpe: Type, reference: Tree, captureIndex: Int): Tree = {
    Apply(
      Select(getModulePath(universe)(captureTagModule), "apply": TermName),
      List(
        reference,
        Literal(
          Constant(captureIndex))))
  }

  /**
   * Deconstructor for {@link scalaxy.internal.CaptureTag#apply} calls in ASTs.
   *  @return if a {@link scalaxy.internal.CaptureTag#apply} call was matched, return some tuple made of the static type of the captured value, the original reference that was captured, and the index of the runtime value of the capture in {@link scalaxy.reified.ReifiedValue#capturedTerms}. Otherwise, return {@link scala.None}.
   */
  def unapply(tree: Tree): Option[(Type, Tree, Int)] = {
    tree match {
      case Apply(
        TypeApply(f, List(tpt)),
        List(
          value,
          Literal(
            Constant(captureIndex: Int)))) if f.symbol == captureTagApplyMethod =>
        Some((tpt.tpe, value, captureIndex))
      case Apply(
        Select(Select(Select(Select(scalaxyName, reifiedName), internalName), captureTagName), applyName),
        List(
          value,
          Literal(
            Constant(captureIndex: Int)))) if List(scalaxyName, reifiedName, internalName, captureTagName).mkString(".") == captureTagFullName &&
        applyName.toString == "apply" =>
        Some((NoType, value, captureIndex))
      case _ =>
        None
    }
  }

  val captureTagFullName = "scalaxy.reified.internal.CaptureTag"
  private lazy val captureTagModule = {
    val s = currentMirror.staticModule(captureTagFullName)
    assert(s != null && s != NoSymbol)
    s
  }

  private lazy val captureTagApplyMethod = {
    val s = captureTagModule.moduleClass.typeSignature.member("apply": TermName)
    assert(s != null && s != NoSymbol)
    s
  }
}
