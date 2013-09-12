package scalaxy.json.base

import scala.language.experimental.macros
import scala.reflect.macros.Context

trait JsonDriverMacros extends MacrosBase {

  type JSONValueType <: AnyRef
  type JSONArrayType <: AnyRef
  type JSONObjectType <: AnyRef
  type JSONFieldType = (String, JSONValueType)

  private[json] def reifyJsonArray(c: Context)(args: List[c.Expr[JSONValueType]]): c.Expr[JSONArrayType]

  private[json] def reifyJsonObject(c: Context)(args: List[c.Expr[(String, JSONValueType)]], containsOptionalFields: Boolean = false): c.Expr[JSONObjectType]

  def isJField(c: Context)(tpe: c.universe.Type): Boolean
  def isJFieldOption(c: Context)(tpe: c.universe.Type): Boolean

  private[json] def reifyJsonValue(c: Context)(v: JSONValueType, replacements: Map[String, (c.universe.Tree, c.universe.Type)]): c.Expr[JSONValueType]
}
