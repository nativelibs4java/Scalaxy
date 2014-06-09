import scala.reflect.macros.blackbox.Context
import language.experimental.macros

object QuasiquoteMacros {
  def unapplyImpl(c: Context)(t: c.Tree) = {
    import c.universe._
    q"""
      new Object {
        def isEmpty = false
        def get = this
        def _1 = {
          println("Getting _1")
          SomeTree
        }
        def _2 = {
          println("Getting _2")
          SomeTree
        }
        def unapply(t: Tree) = this
      }.unapply($t)
    """
  }
}
