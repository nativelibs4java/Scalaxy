package scalaxy.generic

import scala.language.experimental.macros

import scala.reflect._
import scala.reflect.macros.Context
import scala.reflect.runtime.universe

package object internal {

  def methodHomogeneous[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context)(rhs: c.Expr[A]): c.Expr[B] = {
    import c.universe._
    val Apply(Select(target, name), _) = c.macroApplication

    val nameExpr = c.literal(name.toString)
    val prefix = c.prefix.asInstanceOf[c.Expr[GenericOps[A]]]
    reify(prefix.splice.applyDynamic(nameExpr.splice)(rhs.splice).asInstanceOf[B])
    // val res = applyDynamicImpl(c)(c.prefix.asInstanceOf[c.Expr[GenericOps[A]]], c.literal(name.toString), rhs)
    // c.universe.reify(res.splice.asInstanceOf[B])
  }

  def method0[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context): c.Expr[B] = {
    import c.universe._
    val Select(target, name) = c.macroApplication

    val nameExpr = c.literal(name.toString)
    val prefix = c.prefix.asInstanceOf[c.Expr[GenericOps[A]]]
    reify(prefix.splice.selectDynamic(nameExpr.splice).asInstanceOf[B])
    // val res = applyDynamicImpl(c)(c.prefix.asInstanceOf[c.Expr[GenericOps[A]]], c.literal(name.toString))
    // c.universe.reify(res.splice.asInstanceOf[B])
  }
}
