package scalaxy.json.json4s.base

import scalaxy.json.base._

import scala.language.dynamics
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

import org.json4s._

trait Json4sMacros extends JsonDriverMacros {

  type JSONValueType = JValue
  type JSONArrayType = JArray
  type JSONObjectType = JObject

  override def reifyJsonArray(c: Context)(args: List[c.Expr[JValue]]): c.Expr[JArray] = {
    import c.universe._

    val list = c.Expr[List[JValue]]({
      val Apply(TypeApply(Select(target, name), tparams), _) = reify(List[JValue](null)).tree
      Apply(TypeApply(Select(target, name), tparams), args.map(_.tree))
    })
    reify(JArray(list.splice))
  }

  override def reifyJsonObject(c: Context)(args: List[c.Expr[(String, JValue)]], containsOptionalFields: Boolean = false): c.Expr[JObject] = {
    import c.universe._

    val map = c.Expr[List[(String, JValue)]]({
      val Apply(TypeApply(Select(target, name), tparams), _) = reify(List[(String, JValue)](null)).tree
      Apply(TypeApply(Select(target, name), tparams), args.map(_.tree))
    })
    if (containsOptionalFields)
      reify(JObject(map.splice.filter(_ != null)))
    else
      reify(JObject(map.splice))
  }

  override def isJField(c: Context)(tpe: c.universe.Type) = {
    import c.universe._

    val t = tpe.dealias
    t <:< typeOf[JField] ||
    t <:< typeOf[(String, Byte)] ||
    t <:< typeOf[(String, Short)] ||
    t <:< typeOf[(String, Int)] ||
    t <:< typeOf[(String, Long)] ||
    t <:< typeOf[(String, Float)] ||
    t <:< typeOf[(String, Double)] ||
    t <:< typeOf[(String, Boolean)] ||
    t <:< typeOf[(String, String)]
    // TODO
  }
  override def isJFieldOption(c: Context)(tpe: c.universe.Type) = {
    import c.universe._

    val t = tpe.dealias
    t <:< typeOf[None.type] ||
    t <:< typeOf[Option[_]] && {
      val TypeRef(_, _, List(tparam)) = t
      isJField(c)(tparam)
    }
  }
  override def reifyJsonValue(c: Context)(v: JValue, replacements: Map[String, (c.universe.Tree, c.universe.Type)]): c.Expr[JValue] = {
    import c.universe._

    def build(v: JValue): c.Expr[JValue] = v match {
      case JNull =>
        reify(JNull)
      case JNothing =>
        reify(JNothing)
      case JObject(values) =>
        var containsOptionalFields = false
        val fields = values.map({
          case (n, v) =>
            replacements.get(n) match {
              case Some((replacement, tpe)) =>
                if (isJField(c)(tpe)) {
                  c.Expr[JField](replacement)
                } else if (isJFieldOption(c)(tpe)) {
                  val pair = c.Expr[Option[JField]](replacement)
                  containsOptionalFields = true
                  reify(pair.splice.getOrElse(null))
                } else {
                  val key = c.Expr[String](replacement)
                  val value = build(v)
                  reify(key.splice -> value.splice)
                }
              case None =>
                val key = q"$n"
                val value = build(v)
                c.Expr[JField](q"$key -> $value")
            }
        })
        reifyJsonObject(c)(fields, containsOptionalFields = containsOptionalFields)
      case JArray(values) =>
        reifyJsonArray(c)(values.map(build(_)))
      case JString(v) if replacements.contains(v) =>
        c.Expr[JValue](replacements(v)._1)
      case JString(v) =>
        c.Expr[JValue](q"JString($v)")
      case JBool(v) =>
        c.Expr[JValue](q"JBool($v)")
      case JDouble(v) =>
        c.Expr[JValue](q"JDouble($v)")
      case JInt(v) =>
        val x = q"${v.toByteArray}"
        c.Expr[JValue](q"JInt(BigInt($x))")
      case JDecimal(v) =>
        val bd = v.bigDecimal
        val x = reifyByteArray(c)(bd.unscaledValue.toByteArray)
        val s = q"${bd.scale}"
        c.Expr[JValue](q"JDecimal(BigDecimal(BigInt($x), $s))")
    }

    build(v)
  }

  override def reportParsingException(c: Context)(ex: Throwable, posMap: Map[Int, c.universe.Position]): Boolean = {
    ex match {
      case ex: com.fasterxml.jackson.core.JsonParseException =>
        import scala.language.reflectiveCalls
        val pos = ex.getLocation.getCharOffset.asInstanceOf[Int]
        val (from, to) = posMap.toSeq.takeWhile(_._1 <= pos).last
        val msg = ex.getMessage.replaceAll("""(.*?)\s+at \[[^\]]+\]""", "$1")
        c.error(c.enclosingPosition.withPoint(to.start + pos - from), msg)
        true
      case _ =>
        false
    }
  }
}
