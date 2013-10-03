package scalaxy.json.base

import scala.language.dynamics
import scala.language.implicitConversions
import scala.language.experimental.macros

import org.json4s._

private[json] trait PackageBase {

  object json extends Dynamic {
    def applyDynamicNamed(name: String)(args: (String, JValue)*): JValue =
      macro implementation.applyDynamicNamed

    def applyDynamic(name: String)(args: JValue*): JValue =
      macro implementation.applyDynamic
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

  implicit def Byte2JValue(v: Byte) = macro implementation.jdouble[Byte]
  implicit def Short2JValue(v: Short) = macro implementation.jdouble[Short]
  implicit def Int2JValue(v: Int) = macro implementation.jdouble[Int]
  implicit def Long2JValue(v: Long) = macro implementation.jdouble[Long]
  implicit def Double2JValue(v: Double) = macro implementation.jdouble[Double]
  implicit def Float2JValue(v: Float) = macro implementation.jdouble[Float]
  implicit def String2JValue(v: String) = macro implementation.jstring
  implicit def Boolean2JValue(v: Boolean) = macro implementation.jbool
  implicit def Char2JValue(v: Char) = macro implementation.jchar

  implicit def ByteJField(v: (String, Byte)) = macro implementation.jfield[Byte]
  implicit def ShortJField(v: (String, Short)) = macro implementation.jfield[Short]
  implicit def IntJField(v: (String, Int)) = macro implementation.jfield[Int]
  implicit def LongJField(v: (String, Long)) = macro implementation.jfield[Long]
  implicit def DoubleJField(v: (String, Double)) = macro implementation.jfield[Double]
  implicit def FloatJField(v: (String, Float)) = macro implementation.jfield[Float]
  implicit def StringJField(v: (String, String)) = macro implementation.jfield[String]
  implicit def BooleanJField(v: (String, Boolean)) = macro implementation.jfield[Boolean]
  implicit def CharJField(v: (String, Char)) = macro implementation.jfield[Char]
}
