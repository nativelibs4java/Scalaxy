package scalaxy.json.base

import scala.language.dynamics
import scala.language.experimental.macros
import scala.reflect.macros.Context
import org.json4s._
// import org.json4s.jackson.JsonMethods._
// import org.json4s.native.JsonMethods._
import scala.collection.JavaConversions._

trait JSONStringInterpolationMacros extends MacrosBase {
  def parse(str: String, useBigDecimalForDouble: Boolean = false): JValue

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
        c.Expr[JValue](replacements(v))
      case JString(v) =>
        val x = c.literal(v)
        reify(JString(x.splice))
      case JBool(v) =>
        val x = c.literal(v)
        reify(JBool(x.splice))
      case JDouble(v) =>
        val x = c.literal(v)
        reify(JDouble(x.splice))
      case JInt(v) =>
        val x = reifyByteArray(c)(v.toByteArray)
        reify(JInt(BigInt(x.splice)))
      case JDecimal(v) =>
        val bd = v.bigDecimal
        val x = reifyByteArray(c)(bd.unscaledValue.toByteArray)
        val s = c.literal(bd.scale)
        reify(JDecimal(BigDecimal(BigInt(x.splice), s.splice)))
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
      case ex: MatchError =>
        ex.printStackTrace()
        c.error(c.enclosingPosition, ex.getMessage)
        c.literalNull
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
}
