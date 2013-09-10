package scalaxy.json

import scala.language.dynamics
import scala.language.implicitConversions
import scala.language.experimental.macros

import org.json4s._
import org.json4s.jackson.JsonMethods
import com.fasterxml.jackson.core.JsonParser.Feature._
import com.fasterxml.jackson.databind.ObjectMapper

package object jackson extends base.PackageBase {
  implicit class JSONStringContext(val context: StringContext) extends AnyVal {
    def json(args: Any*): JValue =
      macro implementation.json
  }

  def configureLooseSyntaxParser(mapper: ObjectMapper = JsonMethods.mapper) {
    val features = Seq(
      ALLOW_COMMENTS,
      ALLOW_NON_NUMERIC_NUMBERS,
      ALLOW_NUMERIC_LEADING_ZEROS,
      ALLOW_SINGLE_QUOTES,
      ALLOW_UNQUOTED_CONTROL_CHARS,
      ALLOW_UNQUOTED_FIELD_NAMES)
    features.foreach(mapper.configure(_, true))
  }
}
