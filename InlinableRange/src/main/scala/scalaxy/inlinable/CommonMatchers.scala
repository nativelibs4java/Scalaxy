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
  object PositiveIntConstant {
    def unapply(tree: Tree) = Option(tree) collect {
      case Literal(Constant(v: Int)) if v > 0 => v
    }
  }
  object NegativeIntConstant {
    def unapply(tree: Tree) = Option(tree) collect {
      case Literal(Constant(v: Int)) if v < 0 => v
    }
  }
}
