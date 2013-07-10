package scalaxy.reified.impl

import scala.reflect.runtime.universe._
import scala.reflect.runtime.currentMirror

object Capture {
      
  private lazy val captureSymbol = {
    val captureModule = currentMirror.staticModule("scalaxy.reified.impl.Capture")
    captureModule.moduleClass.typeSignature.member("apply": TermName)
  }
  
  def apply[T](ref: T, index: Int): T = ???
  
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
