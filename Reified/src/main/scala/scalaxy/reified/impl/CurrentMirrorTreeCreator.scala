package scalaxy.reified.impl

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._
import scala.reflect.runtime.currentMirror
import scala.reflect.api._

/** TreeCreator that uses {@link scala.reflect.runtime.currentMirror} */
private[reified] case class CurrentMirrorTreeCreator(tree: Tree) extends TreeCreator {
  def apply[U <: Universe with Singleton](m: scala.reflect.api.Mirror[U]): U#Tree = {
    if (m eq currentMirror) {
      tree.asInstanceOf[U#Tree]
    } else {
      throw new IllegalArgumentException(s"Expr defined in current mirror cannot be migrated to other mirrors.")
    }
  }
}
