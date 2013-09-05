package scalaxy

import scala.language.dynamics
import scala.language.implicitConversions
import scala.language.experimental.macros
import scala.reflect.macros.Context
import org.json4s._
import org.json4s.native.JsonMethods._

package object json {
  implicit class JSONStringContext(val context: StringContext) extends AnyVal {
    def json(args: JValue*): JValue =
      macro implementation.json
  }
  object json extends Dynamic {
    def applyDynamicNamed(name: String)(args: (String, JValue)*): JValue =
      macro implementation.applyDynamicNamed

    def applyDynamic(name: String)(args: JValue*): JValue =
      macro implementation.applyDynamic
  }

  implicit def Byte2JValue(v: Byte) = macro implementation.jdouble[Byte]
  implicit def Short2JValue(v: Short) = macro implementation.jdouble[Short]
  implicit def Int2JValue(v: Int) = macro implementation.jdouble[Int]
  implicit def Long2JValue(v: Long) = macro implementation.jdouble[Long]
  implicit def Double2JValue(v: Double) = macro implementation.jdouble[Double]
  implicit def Float2JValue(v: Float) = macro implementation.jdouble[Float]
  implicit def String2JValue(v: String) = macro implementation.jstring
  implicit def Boolean2JValue(v: Boolean) = macro implementation.jbool
}
