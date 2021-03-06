package scalaxy.json.json4s.base

import scalaxy.json.base._
import scala.util.control.NonFatal
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

trait JSONStringInterpolationMacros extends JsonDriverMacros {

  def parse(str: String, useBigDecimalForDouble: Boolean = false): JSONValueType

  def interpolateJsonString(c: Context)(args: c.Expr[Any]*)(implicit tag: c.WeakTypeTag[JSONValueType]): c.Expr[JSONValueType] = {
    import c.universe._
    val Select(Apply(jsonStringContext, List(Apply(Select(scalaStringContext, applyName), fragmentTrees))), jsonName) = c.prefix.tree

    val fragments = fragmentTrees map {
      case t @ Literal(Constant(s: String)) =>
        s -> t.pos
    }

    var typedArgs = args.map(arg => c.typecheck(arg.tree))
    val Placeholders(placeholders, argNames, posMap, _) = ExtractibleJSONStringContext.preparePlaceholders(
      fragments, i => {
        val typedArg = typedArgs(i)
        val tpe = typedArg.tpe
        isJField(c)(tpe) || isJFieldOption(c)(tpe)
      },
      i => typedArgs(i).pos)

    val valNames = (1 to typedArgs.size).map(_ => TermName(c.freshName))
    val valDefs = typedArgs.zip(valNames).map({
      case (typedArg, valName) =>
        ValDef(NoMods, valName, TypeTree(typedArg.tpe), typedArg): Tree
    }).toList
    val replacements: Map[String, (Tree, Type)] =
      typedArgs.zip(valNames).zip(argNames).map({
        case ((typedArg, valName), argName) =>
          argName -> (Ident(valName) -> typedArg.tpe)
      }).toMap

    c.Expr[JSONValueType](
      try   { Block(valDefs, reifyJsonValue(c)(parse(placeholders), replacements).tree) }
      catch { case NonFatal(ex) =>
        if (!reportParsingException(c)(ex, posMap))
          c.error(c.enclosingPosition, ex.getMessage)

        q"null"
      }
    )
  }
}
