package scalaxy.json.base

import scala.language.experimental.macros
import scala.reflect.macros.Context

import org.json4s._

trait JSONStringInterpolationMacros extends MacrosBase {

  def parse(str: String, useBigDecimalForDouble: Boolean = false): JValue

  def jsonApply(c: Context)(args: c.Expr[Any]*): c.Expr[JValue] = {
    import c.universe._

    val Select(Apply(jsonStringContext, List(Apply(Select(scalaStringContext, applyName), fragmentTrees))), jsonName) = c.prefix.tree

    val fragments = fragmentTrees map {
      case t @ Literal(Constant(s: String)) =>
        // StringContext.treatEscapes(s)
        s -> t.pos
    }
    val nameRadix = {
      val concat = fragments.mkString("")
      var i = 0
      def n = "_" + (if (i == 0) "" else i.toString)
      while (concat.contains(n)) {
        i += 1
      }
      n
    }
    var typedArgs = args.map(arg => c.typeCheck(arg.tree))

    val argNames = (1 to typedArgs.size).map(nameRadix + _)
    val valNames = (1 to typedArgs.size).map(_ => c.fresh: TermName)
    val valDefs = typedArgs.zip(valNames).map({
      case (typedArg, valName) =>
        ValDef(NoMods, valName, TypeTree(typedArg.tpe), typedArg): Tree
    }).toList
    val replacements: Map[String, (Tree, Type)] =
      typedArgs.zip(valNames).zip(argNames).map({
        case ((typedArg, valName), argName) =>
          argName -> (Ident(valName) -> typedArg.tpe)
      }).toMap

    val textBuilder = new StringBuilder()
    var posMap = scala.collection.immutable.TreeMap[Int, Int]()
    def addRawText(t: String, pos: Position = null) {
      if (pos != null) {
        posMap += (textBuilder.size -> pos.startOrPoint)
      }
      textBuilder ++= t
    }
    def isJField(tpe: Type) = {
      val t = tpe.normalize
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
    def isJFieldOption(tpe: Type) = {
      val t = tpe.normalize
      t <:< typeOf[None.type] ||
      t <:< typeOf[Option[_]] && {
        val TypeRef(_, _, List(tparam)) = t
        isJField(tparam)
      }
    }

    for (((fragment, fragmentPos), (argName, argTree)) <- fragments.zip(argNames.zip(typedArgs))) {
      addRawText(fragment, fragmentPos)
      val valueSuffix =
        if (isJField(argTree.tpe) || isJFieldOption(argTree.tpe)) ":0"
        else ""
      addRawText("\"" + argName + "\"" + valueSuffix, argTree.pos)
    }
    val (f, p) = fragments.last
    addRawText(f, p)

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
                if (isJField(tpe)) {
                  c.Expr[JField](replacement)
                } else if (isJFieldOption(tpe)) {
                  val pair = c.Expr[Option[JField]](replacement)
                  containsOptionalFields = true
                  reify(pair.splice.getOrElse(null))
                } else {
                  val key = c.Expr[String](replacement)
                  val value = build(v)
                  reify(key.splice -> value.splice)
                }
              case None =>
                val key = c.literal(n)
                val value = build(v)
                reify(key.splice -> value.splice)
            }
        })
        buildJSONObject(c)(fields, containsOptionalFields = containsOptionalFields)
      case JArray(values) =>
        buildJSONArray(c)(values.map(build(_)))
      case JString(v) if replacements.contains(v) =>
        c.Expr[JValue](replacements(v)._1)
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
      val obj = build(parse(textBuilder.toString))
      // val res =
      c.Expr[JValue](
        Block(
          valDefs,
          obj.tree))
      // println("RES: " + res)
      // res
    } catch {
      case ex @ ((_: MatchError) | (_: NullPointerException)) =>
        ex.printStackTrace()
        c.error(c.enclosingPosition, ex.getMessage)
        c.literalNull
      case ex: JacksonParseExceptionType if ex.getClass.getName == "com.fasterxml.jackson.core.JsonParseException" =>
        import scala.language.reflectiveCalls
        val pos = ex.getLocation.getCharOffset.asInstanceOf[Int]
        val (from, to) = posMap.toSeq.takeWhile(_._1 <= pos).last
        val msg = ex.getMessage.replaceAll("""(.*?)\s+at \[[^\]]+\]""", "$1")
        c.error(c.enclosingPosition.withPoint(to + pos - from), msg)
        c.literalNull
      case ex: Throwable =>
        c.error(c.enclosingPosition, ex.getMessage)
        c.literalNull
    }
  }
}
