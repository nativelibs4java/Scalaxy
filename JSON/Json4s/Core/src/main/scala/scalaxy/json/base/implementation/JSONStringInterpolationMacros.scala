package scalaxy.json.json4s.base

import scalaxy.json.base._

import scala.language.experimental.macros
import scala.reflect.macros.Context

trait JSONStringInterpolationMacros extends JsonDriverMacros {

  def parse(str: String, useBigDecimalForDouble: Boolean = false): JSONValueType

  def interpolateJsonString(c: Context)(args: c.Expr[Any]*): c.Expr[JSONValueType] = {
    import c.universe._

    val Select(Apply(jsonStringContext, List(Apply(Select(scalaStringContext, applyName), fragmentTrees))), jsonName) = c.prefix.tree

    val fragments = fragmentTrees map {
      case t @ Literal(Constant(s: String)) =>
        s -> t.pos
    }

    var typedArgs = args.map(arg => c.typeCheck(arg.tree))
    val Placeholders(placeholders, argNames, posMap, _) = ExtractibleJSONStringContext.preparePlaceholders(
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

    try {
      c.Expr[JSONValueType](
        Block(
          valDefs,
          reifyJsonValue(c)(parse(placeholders), replacements).tree))
    } catch {
      case ex: Throwable =>
        if (!reportParsingException(c)(ex, posMap))
          c.error(c.enclosingPosition, ex.getMessage)
        c.literalNull.asInstanceOf[c.Expr[JSONValueType]]
    }
  }
}
