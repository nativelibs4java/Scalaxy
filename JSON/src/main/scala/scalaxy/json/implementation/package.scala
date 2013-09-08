package scalaxy.json

import scala.language.dynamics
import scala.language.experimental.macros
import scala.reflect.macros.Context
import org.json4s._
import org.json4s.jackson.JsonMethods._
// import org.json4s.native.JsonMethods._
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

    val fragments = fragmentTrees map { case t @ Literal(Constant(s: String)) => s -> t.pos }
    val nameRadix = {
      val concat = fragments.map(_._1).mkString("")
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

    val textBuilder = new StringBuilder()
    var posMap = scala.collection.immutable.TreeMap[Int, Int]()
    def addText(t: String, pos: Position = null) {
      if (pos != null) {
        posMap += (textBuilder.size -> pos.startOrPoint)
      }
      textBuilder ++= t
    }
    for (((f, fp), (a, ax)) <- fragments.zip(argNames.zip(args))) {
      addText(f, fp)
      addText("\"" + a + "\"", ax.tree.pos)
    }
    val (f, p) = fragments.last
    addText(f, p)

    def build(v: JValue): c.Expr[JValue] = v match {
      case JNull =>
        reify(JNull)
      case JNothing =>
        reify(JNothing)
      case JObject(values) =>
        buildJSONObject(c)(values.map({ case (n, v) =>
          val key = replacements.get(n).map(c.Expr[String](_)).getOrElse(c.literal(n))
          val value = build(v)
          reify(key.splice -> value.splice)
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
      case JInt(v) =>
        val Apply(TypeApply(target, tparams), _) = reify(Array[Byte](0: Byte)).tree
        val x = c.Expr[Array[Byte]](
          Apply(TypeApply(target, tparams), v.toByteArray.toList.map(c.literal(_).tree))
        )
        reify(JInt(BigInt(x.splice)))
      case JDecimal(v) =>
        val x = c.literal(v.toString)
        reify(JDecimal(BigDecimal(x.splice)))
    }

    type JacksonParseExceptionType = {
      def getLocation: { 
        def getCharOffset: Long
        def getByteOffset: Long
      }
    }
    try {
      build(parse(textBuilder.toString))
    } catch {
      case ex: JacksonParseExceptionType =>
        import scala.language.reflectiveCalls
        val pos = ex.getLocation.getCharOffset.asInstanceOf[Int]
        val (from, to) = posMap.toSeq.takeWhile(_._1 <= pos).last
        c.error(c.enclosingPosition.withPoint(to + pos - from), ex.getMessage)
        c.literalNull
      case ex: Throwable =>
        c.error(c.enclosingPosition, ex.getMessage)
        c.literalNull
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
