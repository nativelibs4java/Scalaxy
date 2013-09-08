package scalaxy.json

import scala.language.dynamics
import scala.language.implicitConversions
import scala.language.experimental.macros
import scala.reflect.macros.Context
import org.json4s._

package object native extends base.PackageBase {
  implicit class JSONStringContext(val context: StringContext) extends AnyVal {
    def json(args: JValue*): JValue =
      macro implementation.json
  }
}
