package scalaxy.json

import scala.language.dynamics
import scala.language.experimental.macros
import scala.reflect.macros.Context
import org.json4s._
import org.json4s.native.JsonMethods._
import scala.collection.JavaConversions._

package object implementation {

  private def checkApplyName(c: Context)(name: c.Expr[String]) {
    import c.universe._

    val Literal(Constant(n)) = name.tree
    if (n != "apply")
      c.error(name.tree.pos, s"value $n is not a member of ${c.prefix.tree.tpe.normalize}")
  }

  private def buildJSONArray(c: Context)
                            (args: List[c.Expr[JValue]]): c.Expr[JArray] = {
    import c.universe._

    val list = c.Expr[List[JValue]]({
      val Apply(TypeApply(Select(target, name), tparams), _) = reify(List[JValue](null)).tree
      Apply(TypeApply(Select(target, name), tparams), args.map(_.tree))
    })
    reify(JArray(list.splice))
  }

  private def buildJSONObject(c: Context)(args: List[c.Expr[(String, JValue)]]): c.Expr[JObject] = {
    import c.universe._

    val map = c.Expr[List[(String, JValue)]]({
      val Apply(TypeApply(Select(target, name), tparams), _) = reify(List[(String, JValue)](null)).tree
      Apply(TypeApply(Select(target, name), tparams), args.map(_.tree))
    })
    reify(new JObject(map.splice))
  }

  def applyDynamicNamed(c: Context)
                       (name: c.Expr[String])
                       (args: c.Expr[(String, JValue)]*): c.Expr[JObject] = {
    checkApplyName(c)(name)
    buildJSONObject(c)(args.toList)
  }

  def applyDynamic(c: Context)
                  (name: c.Expr[String])
                  (args: c.Expr[JValue]*): c.Expr[JArray] = {
    checkApplyName(c)(name)
    buildJSONArray(c)(args.toList)
  }

  def json(c: Context)(args: c.Expr[JValue]*): c.Expr[JValue] = {
    import c.universe._

    val Apply(jsonStringContext, List(Apply(Select(scalaStringContext, applyName), fragmentTrees))) = c.prefix.tree

    val fragments = fragmentTrees map { case Literal(Constant(s: String)) => s }
    val nameRadix = {
      val concat = fragments.mkString("")
      var i = 0
      def n = "_" + (if (i == 0) "" else i.toString)
      while (concat.contains(n)) {
        i += 1
      }
      n
    }
    val argNames = (1 to args.size).map(nameRadix + _)
    val replacements: Map[String, Tree] =
      args.zip(argNames).map({ case (a, n) => n -> a.tree }).toMap
    val text = (fragments.zip(argNames).flatMap({ case (a, b) => Seq(a, "\"" + b + "\"") }) :+ fragments.last).mkString("")

    val buildPair = {
      val Apply(Select(target, name), _) = reify(("", 1: Any)).tree
      (key: c.Expr[String], value: c.Expr[JValue]) => {
        reify(key.splice -> value.splice)
      }
    }

    def build(v: JValue): c.Expr[JValue] = v match {
      case JNull =>
        reify(JNull)
      case JNothing =>
        reify(JNothing)
      case JObject(values) =>
        buildJSONObject(c)(values.map({ case (n, v) =>
          buildPair(
            replacements.get(n).map(c.Expr[String](_)).getOrElse(c.literal(n)),
            build(v))
        }))
      case JArray(values) =>
        buildJSONArray(c)(values.map(build(_)))
      case JString(v) if replacements.contains(v) =>
        val x = c.literal(v)
        reify(JString(x.splice))
      case JString(v) =>
        c.Expr[JValue](replacements(v))
      case JBool(v) =>
        val x = c.literal(v)
        reify(JBool(x.splice))
      case JDouble(v) =>
        val x = c.literal(v)
        reify(JDouble(x.splice))
      // case JInt(v) =>
      //   val x = c.literal(v)
      //   reify(JInt(x.splice)).tree
      // case JDecimal(v) =>
      //   val x = c.literal(v)
      //   reify(JDecimal(x.splice)).tree
    }

    try {
      build(parse(text))
    } catch { case ex: Throwable =>
      c.error(c.enclosingPosition, ex.getMessage)
      // TODO convert position
      null
    }
  }

  def jdouble[A: c.WeakTypeTag](c: Context)(v: c.Expr[A]): c.Expr[JDouble] = {
    import c.universe._
    val Apply(target, List(_)) = reify(JDouble(1)).tree
    c.Expr[JDouble](Apply(target, List(v.tree)))
  }
  def jstring(c: Context)(v: c.Expr[String]): c.Expr[JString] = {
    c.universe.reify(JString(v.splice))
  }
  def jbool(c: Context)(v: c.Expr[Boolean]): c.Expr[JBool] = {
    c.universe.reify(JBool(v.splice))
  }
}
