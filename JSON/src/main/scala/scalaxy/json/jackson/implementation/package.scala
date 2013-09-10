package scalaxy.json
package jackson

import org.json4s._
import org.json4s.jackson.JsonMethods

package object implementation
    extends base.JSONStringInterpolationMacros {

  configureLooseSyntaxParser()

  override def parse(str: String, useBigDecimalForDouble: Boolean): JValue = {
    JsonMethods.parse(str, useBigDecimalForDouble)
  }
}
