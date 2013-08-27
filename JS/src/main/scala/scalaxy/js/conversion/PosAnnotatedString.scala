package scalaxy.js

case class StringPos(start: Int, end: Int) {
  def +(offset: Int): StringPos = StringPos(start + offset, end + offset)
}
case class SourcePos(path: String, line: Int, column: Int)

case class PosAnnotatedString(value: String = "", map: Map[StringPos, SourcePos] = Map()) {
  def +(other: PosAnnotatedString): PosAnnotatedString = {
    val length = value.length
    PosAnnotatedString(
      value + other.value,
      map ++ other.map.toSeq.map {
        case (stringPos, sourcePos) =>
          (stringPos + length, sourcePos)
      }
    )
  }
  def +(text: String): PosAnnotatedString =
    PosAnnotatedString(value + text, map)
}
