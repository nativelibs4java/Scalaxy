package scalaxy.enums

import scala.language.experimental.macros

class EnumValueNames(
  val names: Array[String],
  val initializer: AnyRef => Array[AnyRef])
    extends Serializable

object EnumValueNames {
  implicit def enumValueNames: EnumValueNames =
    macro internal.enumValueNames[EnumValueNames]
}
