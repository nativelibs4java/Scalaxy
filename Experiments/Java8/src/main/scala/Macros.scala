package scalaxy.java8

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

import java.util.function

object impl {
  def javaToFunctionImpl[A: c.WeakTypeTag, B: c.WeakTypeTag, F: c.WeakTypeTag](c: Context)(f: c.Expr[A => B]): c.Expr[F] = {
    import c.universe._

    val atpe = weakTypeTag[A].tpe
    val btpe = weakTypeTag[B].tpe
    val ftpe = weakTypeTag[F].tpe

    val Function(List(param), body) = f.tree

    val methodName: TermName =
      if (btpe <:< typeOf[AnyVal])
        "applyTo" + btpe
      else
        "apply"

    c.Expr[F](q"""
      new ${weakTypeTag[F]}() {
        override def $methodName($param): $btpe = {
          $body
        }
      }
    """)
  }
}
