package scalaxy.reified.impl

import scala.reflect.runtime.universe._
import scala.reflect.runtime.currentMirror

/**
 * Internal methods used for implementation purposes.
 */
object Capture {
      
  private lazy val captureSymbol = {
    val captureModule = currentMirror.staticModule("scalaxy.reified.impl.Capture")
    captureModule.moduleClass.typeSignature.member("apply": TermName)
  }
  
  /** Used to tag captures of external constants or reified values / functions in ASTs.
   *  @param ref original reference to the captured value, kept in the AST for correct typing by the toolbox at runtime.
   *  @param captureIndex index in the captures array of the runtime value of the captured reference
   */
  def apply[T](ref: T, captureIndex: Int): T = ???
  
  /** Deconstructor for Capture.apply call in ASTs.
   *  @return Some(captureIndex) constant param of the Captured.apply call.
   */
  def unapply(tree: Tree): Option[Int] = {
    tree match {
      case 
        Apply(
          TypeApply(f, List(tpe)),
          List(
            value,
            Literal(
              Constant(captureIndex: Int)))) 
                if f.symbol == captureSymbol =>
        Some(captureIndex)
      case _ =>
        None
    }
  }
}
