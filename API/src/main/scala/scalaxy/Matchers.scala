package scalaxy.matchers

import scala.reflect.runtime._
import scala.reflect.runtime.universe._

object IntConstant {
  def unapply(t: Tree): Option[Int] =
    Option(t) collect {
      case Literal(Constant(v: Int)) =>
        v
    }
}
object PositiveIntConstant {
  def unapply(t: Tree) = IntConstant.unapply(t).filter(_ > 0)
}
object NegativeIntConstant {
  def unapply(t: Tree) = IntConstant.unapply(t).filter(_ < 0)
}

object True {
  def unapply(tree: Tree): Boolean =
    tree match {
      case Literal(Constant(true)) =>
        true
      case _ =>
        false
    }
}

object False {
  def unapply(tree: Tree): Boolean =
    tree match {
      case Literal(Constant(false)) =>
        true
      case _ =>
        false
    }
}
