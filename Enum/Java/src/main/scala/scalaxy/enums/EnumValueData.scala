package scalaxy.enums

import scala.language.experimental.macros

class EnumValueData(
  val name: String,
  val ordinal: Int)
    extends Serializable

object EnumValueData {
  implicit def enumValueData: EnumValueData =
    macro internal.enumValueData[EnumValueData]
}
