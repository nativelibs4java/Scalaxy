package scalaxy.json.json4s.base

import scala.language.dynamics
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import org.json4s._
import scala.collection.JavaConversions._

package object implementation
    extends Json4sMacros {

  private def checkApplyName(c: Context)(name: c.Expr[String]) {
    import c.universe._

    val Literal(Constant(n)) = name.tree
    if (n != "apply")
      c.error(name.tree.pos, s"value $n is not a member of ${c.prefix.tree.tpe.dealias}")
  }

  def applyDynamicNamedImpl(c: Context)
                           (name: c.Expr[String])
                           (args: c.Expr[(String, JValue)]*): c.Expr[JObject] = {
    checkApplyName(c)(name)
    reifyJsonObject(c)(args.toList, containsOptionalFields = false)
  }

  def applyDynamicImpl(c: Context)
                      (name: c.Expr[String])
                      (args: c.Expr[JValue]*): c.Expr[JArray] = {
    checkApplyName(c)(name)
    reifyJsonArray(c)(args.toList)
  }

  def jdouble[A: c.WeakTypeTag](c: Context)(v: c.Expr[A]): c.Expr[JDouble] = {
    import c.universe._
    val Apply(target, List(_)) = reify(JDouble(1)).tree
    c.Expr[JDouble](Apply(target, List(v.tree)))
  }

  def jstring(c: Context)(v: c.Expr[String]): c.Expr[JString] = {
    c.universe.reify(JString(v.splice))
  }

  def jchar(c: Context)(v: c.Expr[Char]): c.Expr[JString] = {
    c.universe.reify(JString(v.splice.toString))
  }

  def jbool(c: Context)(v: c.Expr[Boolean]): c.Expr[JBool] = {
    c.universe.reify(JBool(v.splice))
  }

  def jfield[A : c.WeakTypeTag](c: Context)(v: c.Expr[(String, A)]): c.Expr[JField] = {
    import c.universe._

    val tv = c.typecheck(v.tree)
    val n = TermName(c.freshName)
    val vd = ValDef(NoMods, n, TypeTree(tv.tpe), v.tree)

    val key = c.Expr[String](Select(Ident(n), TermName("_1")))
    val value = c.Expr[JValue](Select(Ident(n), TermName("_2")))
    c.Expr[JField](
      Block(
        List(vd),
        reify(key.splice -> value.splice).tree))
  }
}
