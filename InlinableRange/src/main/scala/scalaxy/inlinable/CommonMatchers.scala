package scalaxy.inlinable

import language.experimental.macros
import reflect.makro.Context

trait CommonMatchers
{
  val universe: reflect.makro.Universe
  import universe._
	
  object IntConstant {
    def unapply(tree: Tree) = Option(tree) collect {
      case Literal(Constant(v: Int)) => v
    }
  }
}
