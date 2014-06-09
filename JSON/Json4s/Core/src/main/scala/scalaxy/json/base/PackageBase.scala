package scalaxy.json.json4s.base

import scala.language.dynamics
import scala.language.implicitConversions
import scala.language.experimental.macros

import org.json4s._

private[json] trait PackageBase {
  import implementation._

  object json extends Dynamic {
    def applyDynamicNamed(name: String)(args: (String, JValue)*): JValue =
      macro applyDynamicNamedImpl

    def applyDynamic(name: String)(args: JValue*): JValue =
      macro applyDynamicImpl
  }

  class PreparedJValue(_value: => JValue, _string: => String) {
    lazy val value = _value
    private lazy val string = _string

    override def toString = string
    /** Cannot use string.hashCode, since string content is not stable */
    override def hashCode = value.hashCode
    override def equals(v: Any) = v match {
      case v: PreparedJValue =>
        value.equals(v.value)
      case _ =>
        false
    }
  }
  implicit def preparedJValue2String(p: PreparedJValue): String = p.toString
  implicit def preparedJValue2JValue(p: PreparedJValue): JValue = p.value

  implicit def Byte2JValue(v: Byte): JDouble = macro jdouble[Byte]
  implicit def Short2JValue(v: Short): JDouble = macro jdouble[Short]
  implicit def Int2JValue(v: Int): JDouble = macro jdouble[Int]
  implicit def Long2JValue(v: Long): JDouble = macro jdouble[Long]
  implicit def Double2JValue(v: Double): JDouble = macro jdouble[Double]
  implicit def Float2JValue(v: Float): JDouble = macro jdouble[Float]
  implicit def String2JValue(v: String): JString = macro jstring
  implicit def Boolean2JValue(v: Boolean): JBool = macro jbool
  implicit def Char2JValue(v: Char): JString = macro jchar

  implicit def ByteJField(v: (String, Byte)): JField = macro jfield[Byte]
  implicit def ShortJField(v: (String, Short)): JField = macro jfield[Short]
  implicit def IntJField(v: (String, Int)): JField = macro jfield[Int]
  implicit def LongJField(v: (String, Long)): JField = macro jfield[Long]
  implicit def DoubleJField(v: (String, Double)): JField = macro jfield[Double]
  implicit def FloatJField(v: (String, Float)): JField = macro jfield[Float]
  implicit def StringJField(v: (String, String)): JField = macro jfield[String]
  implicit def BooleanJField(v: (String, Boolean)): JField = macro jfield[Boolean]
  implicit def CharJField(v: (String, Char)): JField = macro jfield[Char]
}
