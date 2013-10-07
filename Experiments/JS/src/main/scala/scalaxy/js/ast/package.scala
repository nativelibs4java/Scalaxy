package scalaxy.js

package object ast {
  val NoSourcePos = SourcePos(null, -1, -1)

  implicit class Positions(c: StringContext) {
    def a(args: Any*): PosAnnotatedString = PosAnnotatedString(c.s(args:_*))
  }
}
