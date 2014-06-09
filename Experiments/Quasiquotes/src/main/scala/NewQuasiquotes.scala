import scala.reflect.macros.blackbox.Context
import language.experimental.macros

trait Tree
case object SomeTree extends Tree

object NewQuasiquotes {
  implicit class QuasiquoteInterpolation(c: StringContext) {
    object nq {
      def unapply(t: Tree) = macro QuasiquoteMacros.unapplyImpl
    }
  }
}
