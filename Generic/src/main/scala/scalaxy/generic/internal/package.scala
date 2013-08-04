package scalaxy.generic

import scala.language.experimental.macros

import scala.reflect._
import scala.reflect.macros.Context
import scala.reflect.runtime.universe

package object internal {

  def checkStaticConstraint[A: c.WeakTypeTag](c: Context)(): c.Expr[Unit] = {
    // println(s"ConstraintOnA: ${weakTypeTag[ConstraintOnA].tpe}")
    c.literalUnit
  }

  def applyDynamic[A: c.WeakTypeTag](c: Context)(name: c.Expr[String])(args: c.Expr[Any]*): c.Expr[Any] = {
    applyDynamicImpl(c)(c.prefix.asInstanceOf[c.Expr[GenericOps[A]]], name, args: _*)
  }

  private def applyDynamicImpl[A: c.WeakTypeTag](c: Context)(prefix: c.Expr[GenericOps[A]], name: c.Expr[String], args: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    // TODO: If the type of Generic is concrete, then:
    // - check that method exists in A with the static types of these args
    // - if prefix is GenericOps.apply[A](value), replace by value, otherwise replace by prefix.value
    val pref = c.prefix.asInstanceOf[c.Expr[GenericOps[A]]]
    val Apply(target, firstArgs) = c.universe.reify(GenericOps.applyDynamicImpl(pref.splice, name.splice)).tree
    c.Expr[Any](
      Apply(target, firstArgs ++ args.map(_.tree))
    )
  }

  def selectDynamic[A: c.WeakTypeTag](c: Context)(name: c.Expr[String]): c.Expr[Any] = {
    import c.universe._
    // TODO: If the type of Generic is concrete, then:
    // - check that method exists in A with the static types of these args
    // - if prefix is GenericOps.apply[A](value), replace by value, otherwise replace by prefix.value
    val pref = c.prefix.asInstanceOf[c.Expr[GenericOps[A]]]
    c.universe.reify(
      GenericOps.selectDynamicImpl(pref.splice, name.splice)
    )
  }

  def updateDynamic[A: c.WeakTypeTag](c: Context)(name: c.Expr[String])(value: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._
    // TODO: If the type of Generic is concrete, then:
    // - check that method exists in A with the static types of these args
    // - if prefix is GenericOps.apply[A](value), replace by value, otherwise replace by prefix.value
    val pref = c.prefix.asInstanceOf[c.Expr[GenericOps[A]]]
    c.universe.reify(
      GenericOps.updateDynamicImpl(pref.splice, name.splice, value.splice)
    )
  }

  def methodHomogeneous[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context)(rhs: c.Expr[A]): c.Expr[B] = {
    import c.universe._
    val Apply(Select(target, name), _) = c.macroApplication

    val res = applyDynamicImpl(c)(c.prefix.asInstanceOf[c.Expr[GenericOps[A]]], c.literal(name.toString), rhs)
    c.universe.reify(res.splice.asInstanceOf[B])
  }

  def method0[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context): c.Expr[B] = {
    import c.universe._
    val Select(target, name) = c.macroApplication

    val res = applyDynamicImpl(c)(c.prefix.asInstanceOf[c.Expr[GenericOps[A]]], c.literal(name.toString))
    c.universe.reify(res.splice.asInstanceOf[B])
  }
}
