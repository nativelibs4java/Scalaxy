package scalaxy

import scala.language.experimental.macros
import scala.language.implicitConversions
import scala.language.dynamics

import scala.reflect._
import scala.reflect.macros.Context

import scala.annotation.implicitNotFound

package union {

  private[union] trait Trait
}

package object union {

  def prove[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context): c.Expr[B] = {
    import c.universe._

    val a = c.universe.weakTypeTag[A].tpe
    val b = c.universe.weakTypeTag[B].tpe
    // println("### proving: " + a + " as " + b)

    // b.normalize match {
    //   case TypeRef(_, _, List(tparam)) =>
    if (b <:< typeOf[_ <|< _]) {
      val TypeRef(_, _, List(aa, constraint)) = b.normalize.baseType(typeOf[<|<[_, _]].typeSymbol)
      println("### constraint: " + constraint)
      internal.checkType(c)(a, constraint, _ <:< _, "match or derive from")
    } else if (b <:< typeOf[_ =|= _]) {
      val TypeRef(_, _, List(aa, constraint)) = b.normalize.baseType(typeOf[=|=[_, _]].typeSymbol)
      println("### constraint: " + constraint)
      internal.checkType(c)(a, constraint, _ =:= _, "match")
    } else {
      c.error(c.prefix.tree.pos, "Cannot prove " + a + " as " + b + " (unknown type)")
    }
    //   case _ =>
    //     c.error(c.prefix.tree.pos, "Cannot prove " + a + " as " + b + " (not a type-class, expecting a type with exactly one type parameter)")
    // }
    c.literalNull.asInstanceOf[c.Expr[B]]
  }

  def wrap[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context)(value: c.Expr[A]): c.Expr[B] = {
    import c.universe._

    val a = weakTypeTag[A].tpe
    val b = weakTypeTag[B].tpe

    internal.checkType(c)(a, b, _ =:= _, "match")

    val valueExpr = value
    val expr = reify {
      new Trait {
        val value: Any = valueExpr.splice
      }
    }
    // println("expr = " + expr)
    val replaced = expr.tree.substituteSymbols(
      List(typeOf[Trait].typeSymbol),
      List(b.typeSymbol)
    )
    // println(replaced)
    c.Expr[B](replaced)
    //c.literalNull.asInstanceOf[c.Expr[B]]
  }

  // def wrap[T](value: Any) = macro wrap[T]
  implicit class AnyOps[A](value: A) {
    def as[B]: B = macro internal.as[A, B]
  }
}
