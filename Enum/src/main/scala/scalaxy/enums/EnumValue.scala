// package scalaxy.enums

// class EnumValue(implicit data: EnumValueData)
//     extends java.lang.Enum[EnumValue](
//       if (data == null) "?" else data.name,
//       if (data == null) -1 else data.ordinal)
//     // with java.io.Externalizable 
// {

//   // def readExternal(in: java.io.ObjectInput) {
//   //   ???
//   // }
//   // def writeExternal(out: java.io.ObjectOutput) {
//   //   ???
//   // }
//   // def writeReplace(): Object = {
//   //   SerializableValue(name)
//   // }

//   // /**
//   //  * See http://docs.oracle.com/javase/7/docs/platform/serialization/spec/input.html#5903
//   //  */
//   // private def readResolve(): Object = {
//   //   val value = valuesArray(ordinal)
//   //   if (value.name != name) {
//   //     sys.error(s"Failed to deserialize value with name $name and ordinal $ordinal: existing item at same ordinal has name ${value.name}")
//   //   }
//   //   value
//   // }
// }
