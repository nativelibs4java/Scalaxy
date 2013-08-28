package scalaxy

package object js {
  implicit class DynamicExtension(value: Any) {
    def asDynamic = new DynamicValue(value)
  }
  type JSObject = collection.mutable.Map[String, Any]

  implicit class JSStringContext(c: StringContext) {
    def js(args: Any*): Any = ???
  }

  import java.io._
  private [js] def write(text: String, file: File) {
    file.getParentFile.mkdirs()
    val out = new PrintWriter(file)
    out.println(text)
    out.close()
    println("Wrote " + file.getAbsolutePath)
  }
}
