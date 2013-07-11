package scalaxy.reified.impl

import scala.reflect.runtime.universe._
import scala.reflect.runtime.currentMirror

/**
 * Internal methods used for implementation purposes.
 */
object CaptureTag {
  /** Symbol of CaptureTag.apply method */
  private lazy val captureSymbol = {
    val captureModule = currentMirror.staticModule("scalaxy.reified.impl.CaptureTag")
    captureModule.moduleClass.typeSignature.member("apply": TermName)
  }
  
  /** Used to tag captures of external constants or reified values / functions in ASTs.
   *  @param ref original reference to the captured value, kept in the AST for correct typing by the toolbox at runtime.
   *  @param captureIndex index in the captures array of the runtime value of the captured reference
   */
  def apply[T](ref: T, captureIndex: Int): T = ???
  
  /** Construct the AST for the capture tag call that corresponds to these params. */
  def construct(tpe: Type, value: Tree, captureIndex: Int): Tree = {
    Apply(
      TypeApply(Ident(captureSymbol), List(TypeTree(tpe))),
      List(
        value,
        Literal(
          Constant(captureIndex))))
  }
  
  /** Deconstructor for CaptureTag.apply call in ASTs.
   *  @return Some(captureIndex) constant param of the Captured.apply call.
   */
  def unapply(tree: Tree): Option[(Type, Tree, Int)] = {
    tree match {
      case 
        Apply(
          TypeApply(f, List(tpt)),
          List(
            value,
            Literal(
              Constant(captureIndex: Int)))) 
                if f.symbol == captureSymbol =>
        Some((tpt.tpe, value, captureIndex))
      case _ =>
        None
    }
  }
}
