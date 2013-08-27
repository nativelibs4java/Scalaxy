package scalaxy

package object js {
  implicit class DynamicExtension(value: Any) {
    def asDynamic = new DynamicValue(value)
  }
  type JSObject = collection.mutable.Map[String, Any]
}
