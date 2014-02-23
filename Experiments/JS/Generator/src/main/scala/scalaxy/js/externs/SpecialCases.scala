package scalaxy.js

import scala.collection.JavaConversions._
import scala.reflect.api.Universe
import com.google.javascript.jscomp._
import com.google.javascript.rhino._
import com.google.javascript.rhino.jstype._

object SpecialCases {

  val qualNameRx = """(.*?)\.([^.]+)""".r
  // val jsPrimitiveTypes = Set("Object", "Boolean", "String", 
  val invalidOverrideExceptions = Set(
    "DOMApplicationCache.prototype.addEventListener",
    "DOMApplicationCache.prototype.removeEventListener",
    "DOMApplicationCache.prototype.dispatchEvent",
    // "ClipboardData.prototype.clearData",
    "DataTransfer.prototype.clearData",
    "DataTransfer.prototype.setData",
    "DataTransfer.prototype.getData",
    "Object.prototype.toLocaleString",
    "Object.prototype.toSource",
    "Boolean.prototype.toSource",
    "Date.prototype.toJSON",
    "Element.prototype.querySelector"
  )//.map(_.r)
  val missingOverrideExceptions = Set(
    "String.prototype.valueOf",
    "Date.prototype.valueOf",
    "Array.prototype.toSource"
  )//.map(_.r)
}
