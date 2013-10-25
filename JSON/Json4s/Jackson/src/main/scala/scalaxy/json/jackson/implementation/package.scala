package scalaxy.json.json4s
package jackson

import scala.language.experimental.macros
import scala.reflect.macros.Context

import org.json4s._
import org.json4s.jackson.JsonMethods

package object implementation
    extends base.JSONStringInterpolationMacros
    with base.Json4sMacros {

  configureLooseSyntaxParser()

  override def parse(str: String, useBigDecimalForDouble: Boolean): JValue =
    JsonMethods.parse(str, useBigDecimalForDouble)

  def jsonApply(c: Context)(args: c.Expr[Any]*): c.Expr[JValue] =
    interpolateJsonString(c)(args: _*)
}
