package scalaxy.json
package native

import org.json4s._
import org.json4s.native.JsonMethods

package object implementation extends base.JSONStringInterpolationMacros {
  override def parse(str: String): JValue = {
    JsonMethods.parse(str)
  }
}
