package scalaxy.reified.impl

import scalaxy.reified.ReifiedValue

import scala.reflect.runtime.universe._
import scala.collection.immutable

/**
 * Conversions for captured references of common types. 
 */
object CaptureConversions {
  type Conversion = PartialFunction[(Any, Any/*full Conversion*/), Tree]
  final val CONSTANT: Conversion = {
    case (value @ (
        (_: Number) | 
        (_: java.lang.Boolean) | 
        (_: String) | 
        (_: java.lang.Character)), _) =>
      Literal(Constant(value))
  }
  
  final val REIFIED_VALUE: Conversion = {
    case (value: ReifiedValue[_], fullConversion: Conversion) =>
      value.expr(fullConversion).tree.duplicate
  }
  
  final val DEFAULT: Conversion = {
    CONSTANT orElse REIFIED_VALUE
  }
  
  // TODO: convert immutable array, seq, list, set, map
  /*
  final val ARRAY: Conversion = {
    case array: Array[_] =>
  }
  final val IMMUTABLE_COLLECTION: Conversion = {
    case set: immutable.Set[_] =>
    case map: immutable.Map[_] =>
    case list: immutable.List[_] =>
    case vector: immutable.Vector[_] =>
    case stack: immutable.Stack[_] =>
    case seq: immutable.Seq[_] =>
  }
  */
}
