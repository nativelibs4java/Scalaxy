package scalaxy.json

import scala.language.dynamics
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import scala.util.parsing.json._

package object implementation {
  private def checkApplyName(c: Context)(name: c.Expr[String]) {
    import c.universe._

    val Literal(Constant(n)) = name.tree
    if (n != "apply")
      c.error(name.tree.pos, s"value $n is not a member of ${c.prefix.tree.tpe.dealias}")
  }

  private def buildJSONArray(c: Context)(args: List[c.universe.Tree]) = {
    import c.universe._

    val list = c.Expr[List[Any]]({
      val Apply(TypeApply(Select(target, name), tparams), _) = reify(List[Any](1)).tree
      Apply(TypeApply(Select(target, name), tparams), args)
    })
    reify(new JSONArray(list.splice))
  }

  private def buildJSONObject(c: Context)(args: List[c.universe.Tree]) = {
    import c.universe._

    val map = c.Expr[Map[String, Any]]({
      val Apply(TypeApply(Select(target, name), tparams), Nil) = reify(Map[String, Any]()).tree
      Apply(TypeApply(Select(target, name), tparams), args)
    })
    reify(new JSONObject(map.splice))
  }

  def applyDynamicNamed(c: Context)(name: c.Expr[String])(args: c.Expr[(String, Any)]*): c.Expr[JSONObject] = {
    checkApplyName(c)(name)
    buildJSONObject(c)(args.toList.map(_.tree))
  }

  def applyDynamic(c: Context)(name: c.Expr[String])(args: c.Expr[Any]*): c.Expr[JSONArray] = {
    checkApplyName(c)(name)
    buildJSONArray(c)(args.toList.map(_.tree))
  }

  def json(c: Context)(args: c.Expr[Any]*): c.Expr[JSONObject] = {
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
    val replacements = args.zip(argNames).map({ case (a, n) => n -> a.tree }).toMap
    val text = (fragments.zip(argNames).flatMap({ case (a, b) => Seq(a, "\"" + b + "\"") }) :+ fragments.last).mkString("")

    def buildPair(v: (String, Any)): Tree =
      reify(("", 1: Any)).tree match {
        case Apply(Select(target, name), _) =>
          Apply(Select(target, name), List(build(v._1), build(v._2)))
      }

    def buildMap(v: Map[String, Any]): Tree =
      buildJSONObject(c)(v.toList.map(build(_))).tree

    def buildArray(v: List[Any]): Tree =
      buildJSONArray(c)(v.map(build(_))).tree

    def build(v: Any): Tree = v match {
      case v: (String, Any) =>
        buildPair(v)
      case v: Map[String, Any] =>
        buildMap(v)
      case v: List[Any] =>
        buildArray(v)
      case v: String if replacements.contains(v) =>
        replacements(v)
      case v @ ((_: String) | (_: Double) | (_: Float) | (_: Byte) | (_: Short) | (_: Int) | (_: Long) | (_: Number)) =>
        Literal(Constant(v))
    }

    val result = JSON.parseFull(text) match {
      case Some(v: Map[String, Any]) =>
        c.Expr[JSONObject](build(v))
      case v =>
        c.error(c.enclosingPosition, "Expected object, got " + v)
        null
    }
    // println("JSON: " + js)
    // val result = build(js)
    // println("RESULT: " + result)
    result
  }
}
