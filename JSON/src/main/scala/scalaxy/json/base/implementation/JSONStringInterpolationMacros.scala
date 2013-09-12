package scalaxy.json.base

import scala.language.experimental.macros
import scala.reflect.macros.Context

// import org.json4s._

trait JSONStringInterpolationMacros extends JsonDriverMacros {

  def parse(str: String, useBigDecimalForDouble: Boolean = false): JSONValueType

  def interpolateJsonString(c: Context)(args: c.Expr[Any]*): c.Expr[JSONValueType] = {
    import c.universe._

    val Select(Apply(jsonStringContext, List(Apply(Select(scalaStringContext, applyName), fragmentTrees))), jsonName) = c.prefix.tree

    val fragments = fragmentTrees map {
      case t @ Literal(Constant(s: String)) =>
        // StringContext.treatEscapes(s)
        s -> t.pos
    }

    var typedArgs = args.map(arg => c.typeCheck(arg.tree))
    val (placeholders, argNames, posMap) = ExtractibleJSONStringContext.preparePlaceholders(
      fragments, i => {
        val typedArg = typedArgs(i)
        val tpe = typedArg.tpe
        isJField(c)(tpe) || isJFieldOption(c)(tpe)
      },
      i => typedArgs(i).pos)

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

    type JacksonParseExceptionType = {
      def getLocation: {
        def getCharOffset: Long
        def getByteOffset: Long
      }
    }
    try {
      val obj = reifyJsonValue(c)(parse(placeholders), replacements)//parse(textBuilder.toString))
      // val res =
      c.Expr[JSONValueType](
        Block(
          valDefs,
          obj.tree))
      // println("RES: " + res)
      // res
    } catch {
      case ex @ ((_: MatchError) | (_: NullPointerException)) =>
        ex.printStackTrace()
        c.error(c.enclosingPosition, ex.getMessage)
        c.literalNull.asInstanceOf[c.Expr[JSONValueType]]
      case ex: JacksonParseExceptionType if ex.getClass.getName == "com.fasterxml.jackson.core.JsonParseException" =>
        import scala.language.reflectiveCalls
        val pos = ex.getLocation.getCharOffset.asInstanceOf[Int]
        val (from, to) = posMap.toSeq.takeWhile(_._1 <= pos).last
        val msg = ex.getMessage.replaceAll("""(.*?)\s+at \[[^\]]+\]""", "$1")
        c.error(c.enclosingPosition.withPoint(to.startOrPoint + pos - from), msg)
        c.literalNull.asInstanceOf[c.Expr[JSONValueType]]
      case ex: Throwable =>
        c.error(c.enclosingPosition, ex.getMessage)
        c.literalNull.asInstanceOf[c.Expr[JSONValueType]]
    }
  }
}
