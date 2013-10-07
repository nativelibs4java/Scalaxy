package scalaxy

import scala.language.dynamics
import scala.language.experimental.macros
import scala.reflect.macros.Context
import scala.util.parsing.json._
// import com.fasterxml.jackson.databind._

/*
Jackson JavaDoc:
http://fasterxml.github.io/jackson-databind/javadoc/2.2.0/index.html

*/
package object json {
  implicit class JSONStringContext(val context: StringContext) extends AnyVal {
    def json(args: Any*): JSONObject =
      macro implementation.json
  }
  object json extends Dynamic {
    def applyDynamicNamed(name: String)(args: (String, Any)*): JSONObject =
      macro implementation.applyDynamicNamed

    def applyDynamic(name: String)(args: Any*): JSONArray =
      macro implementation.applyDynamic
  }
}
