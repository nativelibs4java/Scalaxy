package scalaxy ; package plugin
import language.existentials

trait ExprTreeFixer {
  val universe: scala.reflect.api.Universe

  type TreeWithWritableType = {
    var tpe: universe.Type
  }

  def fixTypedExpression[T](name: String, x: universe.Expr[T]) = {
    if (x.tree.tpe == null) {
      if (x.staticTpe != null) {
        //x.tree.tpe = x.staticTpe
        x.tree.asInstanceOf[TreeWithWritableType].tpe = x.staticTpe
        //println("Fixed pattern tree type for '" + name + "' :\n\t" + x.tree + ": " + x.tree.tpe)
      } else {
        println("Failed to fix pattern tree typefor '" + name + "' :\n\t" + x.tree)
      }
    }
  }
}
