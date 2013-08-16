package scalaxy.enums

import scala.reflect.macros.Context
import scala.language.experimental.macros

class enum(implicit enumValueNames: EnumValueNames) 
    {//extends DelayedInit{

  type value = EnumValue
  // type value <: EnumValue

  private lazy val valuesArray: Array[value] = {
    // println("INITIALIZING")
    enumValueNames.initializer(this).map(_.asInstanceOf[value])
  }

  private lazy val namesMap: Map[String, value] = {
    valuesArray.map(value => value.name -> value).toMap
  }

  private var nextOrdinal = 0
  protected def nextEnumValueData: EnumValueData = {
    val ordinal = nextOrdinal
    // println("Next " + ordinal + ", size = " + enumValueNames.names.size)
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
      extends java.lang.Enum[value](
        if (data == null) "?" else data.name,
        if (data == null) -1 else data.ordinal) {

    /**
     * See http://docs.oracle.com/javase/7/docs/platform/serialization/spec/input.html#5903
     */
    private def readResolve(): Object = {
      val value = valuesArray(ordinal)
      if (value.name != name) {
        sys.error(s"Failed to deserialize value with name $name and ordinal $ordinal: existing item at same ordinal has name ${value.name}")
      }
      value
    }
  }

  def value: value =
    new value()(nextEnumValueData)

  def values: Array[value] =
    valuesArray.clone.asInstanceOf[Array[value]]

  def valueOf(name: String): value =
    namesMap.get(name).getOrElse {
      sys.error("No such value in enum: " + name)
    }

  // def delayedInit(body: => Unit) = {
  //   println("DELAYED: " + this)
  //   body
  //   valuesArray
  // }
}
