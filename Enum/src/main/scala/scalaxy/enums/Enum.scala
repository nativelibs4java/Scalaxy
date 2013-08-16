package scalaxy.enums

import scala.reflect.macros.Context
import scala.language.experimental.macros

class enum(implicit enumValueNames: EnumValueNames) {
  type value = EnumValue
  // type value <: EnumValue

  private var sealedEnum = false
  private val valuesBuffer = collection.mutable.ArrayBuffer[value]()

  private var nextOrdinal = 0
  protected def nextEnumValueData: EnumValueData = {
    val ordinal = nextOrdinal
    if (ordinal >= enumValueNames.names.size) {
      null
    } else {
      nextOrdinal += 1
      new EnumValueData(
        enumValueNames.names(ordinal),
        ordinal
      )
    }
  }

  class EnumValue(implicit data: EnumValueData)
      extends java.lang.Enum[value](data.name, data.ordinal) {

    if (data.ordinal == valuesBuffer.size) {
      if (sealedEnum) {
        sys.error("Enum is now sealed!")
      }
      valuesBuffer += this
    } else if (data.ordinal < valuesBuffer.size) {
      val existing = valuesBuffer(data.ordinal)
      if (existing.name != data.name) {
        sys.error("Mismatching names at ordinal " + ordinal + ": " + existing.name + " vs. " + data.name)
      }
    } else {
      sys.error("Invalid enum value with name " + data.name + " and out of range ordinal " + data.ordinal)
    }
  }

  enumValueNames.initializer(this)

  def value: value =
    new value()(nextEnumValueData)

  private lazy val namesMap: Map[String, value] = {
    sealedEnum = true
    valuesBuffer.map(value => value.name -> value).toMap
  }

  def values: Array[value] =
    valuesBuffer.toArray

  def valueOf(name: String): value =
    namesMap.get(name).getOrElse {
      sys.error("No such value in enum: " + name)
    }
}
