package scalaxy.json
package jackson

import scala.language.dynamics
import scala.language.experimental.macros
import scala.reflect.macros.Context
import org.json4s._
import org.json4s.jackson.JsonMethods
// import org.json4s.native.JsonMethods._
import scala.collection.JavaConversions._

package object implementation extends base.JSONStringInterpolationMacros  {
  configureLooseParser

  override def parse(str: String, useBigDecimalForDouble: Boolean): JValue = {
    JsonMethods.parse(str, useBigDecimalForDouble)
  }
}
