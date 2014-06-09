package scalaxy.json.json4s

import scala.language.dynamics
import scala.language.implicitConversions
import scala.language.experimental.macros

import org.json4s._
import org.json4s.jackson.{ JsonMethods, Json4sScalaModule }
import com.fasterxml.jackson.core.JsonParser.Feature._
import com.fasterxml.jackson.databind.ObjectMapper

package object jackson extends base.PackageBase {
  import implementation._
  
  private lazy val mapper = {
    val mapper = new ObjectMapper
    mapper.registerModule(new Json4sScalaModule)
    configureLooseSyntaxParser(mapper)
    mapper
  }

  implicit class JSONStringContext(val context: StringContext) {
    object json extends base.ExtractibleJSONStringContext(context) {
      def apply(args: Any*): JValue =
        macro jsonApply

      override def parse(str: String): JValue = {
        // JsonMethods.parse(str)
        mapper.readValue(str, classOf[JValue])
      }
    }
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
