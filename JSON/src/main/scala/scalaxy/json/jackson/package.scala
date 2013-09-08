package scalaxy.json

import scala.language.dynamics
import scala.language.implicitConversions
import scala.language.experimental.macros
import scala.reflect.macros.Context
import org.json4s._

package object jackson extends base.PackageBase {
  implicit class JSONStringContext(val context: StringContext) extends AnyVal {
    def json(args: JValue*): JValue =
      macro implementation.json
  }

  def configureLooseParser {
    import org.json4s.jackson.JsonMethods.mapper
    import com.fasterxml.jackson.core.JsonParser.Feature._
    for (feature <- Seq(
        ALLOW_COMMENTS,
        ALLOW_NON_NUMERIC_NUMBERS,
        ALLOW_NUMERIC_LEADING_ZEROS,
        ALLOW_SINGLE_QUOTES,
        ALLOW_UNQUOTED_CONTROL_CHARS,
        ALLOW_UNQUOTED_FIELD_NAMES)) {
      mapper.configure(feature, true)
    }
  }

  configureLooseParser
}
