package scalaxy.json
package native

import scala.language.experimental.macros
import scala.reflect.macros.Context

import org.json4s._
import org.json4s.native.JsonMethods

package object implementation
    extends base.JSONStringInterpolationMacros
    with base.Json4sMacros {

  override def parse(str: String, useBigDecimalForDouble: Boolean): JValue =
    JsonMethods.parse(str, useBigDecimalForDouble)

  def jsonApply(c: Context)(args: c.Expr[Any]*): c.Expr[JValue] =
    interpolateJsonString(c)(args: _*)
}
