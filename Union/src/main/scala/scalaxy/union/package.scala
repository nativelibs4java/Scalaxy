package scalaxy

import scala.language.experimental.macros
import scala.reflect.macros.Context

package union {
  private[union] trait Trait
}

package object union {

  implicit class AnyOps[A](value: A) {
    def as[B]: B = macro internal.as[A, B]
  }

  def wrap[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context)(value: c.Expr[A]): c.Expr[B] = {
    import c.universe._

    internal.checkMemberOfUnion[A, B](c)

    val valueExpr = value
    val expr = reify {
      new Trait {
        val value: Any = valueExpr.splice
      }
    }
    c.Expr[B](
      expr.tree.substituteSymbols(
        List(typeOf[Trait].typeSymbol),
        List(weakTypeTag[B].tpe.typeSymbol)
      )
    )
  }
}
