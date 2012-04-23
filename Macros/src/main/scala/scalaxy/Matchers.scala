package scalaxy.matchers

import scala.reflect.mirror._

object PositiveConstant {
  def unapply(tree: Tree): Option[Int] =
    Option(tree) collect {
      case Literal(Constant(v: Int)) if v > 0 =>
        v
    }
}
object NegativeConstant {
  def unapply(tree: Tree): Option[Int] =
    Option(tree) collect {
      case Literal(Constant(v: Int)) if v < 0 =>
        v
    }
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
