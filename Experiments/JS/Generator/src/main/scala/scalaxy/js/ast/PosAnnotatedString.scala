package scalaxy.js.ast

case class StringPos(start: Int, end: Int) {
  def +(offset: Int) =
    StringPos(start + offset, end + offset)
}
case class SourcePos(path: String, line: Int, column: Int) {
  def apply(content: String) =
    PosAnnotatedString(content, Map(StringPos(0, content.length) -> this))
}

case class PosAnnotatedString(value: String = "", map: Map[StringPos, SourcePos] = Map()) {
  def ++(other: PosAnnotatedString) = {
    val length = value.length
    if (length == 0)
      other
    else if (other.value.length == 0)
      this
    else
      PosAnnotatedString(
        value + other.value,
        map ++ other.map.toSeq.map {
          case (stringPos, sourcePos) =>
            (stringPos + length, sourcePos)
        })
  }
  // def +(text: String) =
  //   PosAnnotatedString(value + text, map)
}
