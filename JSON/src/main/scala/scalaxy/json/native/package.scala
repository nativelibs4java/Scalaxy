package scalaxy.json

import scala.language.experimental.macros

import org.json4s._

package object native extends base.PackageBase {
  implicit class JSONStringContext(val context: StringContext) {
    object json {
      def apply(args: Any*): JValue =
        macro implementation.jsonApply

      def unapply(subpatterns: Any*): Option[JValue] =
        macro implementation.jsonUnapply
    }
  }
}
