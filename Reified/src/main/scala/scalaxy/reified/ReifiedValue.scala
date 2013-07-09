package scalaxy.reified

import scala.language.experimental.macros

import scala.reflect._
import scala.reflect.macros.Context
import scala.reflect.runtime
import scala.tools.reflect.ToolBox

class ReifiedValue[A](
    val value: A,
    private[reified] val rawExpr: runtime.universe.Expr[A],
    val captures: Seq[AnyRef]) {
  val expr = {
    // TODO: replace captures
    rawExpr
  }
}
