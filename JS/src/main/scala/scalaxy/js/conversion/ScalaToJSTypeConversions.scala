package scalaxy.js

import scala.reflect.api.Universe

trait ScalaToJSTypeConversions {

  val global: Universe
  import global._

  def runtimeObjectClassName: String

  private lazy val functionRx = """scala\.Function(\d+)""".r
  private lazy val arrayRx = """scala\.Array\[.*""".r
  def eraseType(tpe: Type): String = {
    if (tpe == NoType) "undefined"
    else Option(tpe).map(_.normalize).map(_.typeSymbol.fullName.toString match {
      case "scala.Unit" | "scala.Nothing" =>
        "undefined"
      case "scala.Char" =>
        "string"
      case "scala.Boolean" =>
        "boolean"
      case "scala.AnyVal" =>
        "number"
      case "java.lang.Character" | "java.lang.String" =>
        "?string"
      case "java.lang.Boolean" =>
        "?boolean"
      case "java.lang.Number" =>
        "?number"
      case functionRx(n) =>
        "Function" // TODO args and return type
      case arrayRx() =>
        "Array.<" +
        (tpe match {
          case TypeRef(_, _, List(elementTpe)) =>
            eraseType(elementTpe)
        }) +
        ">"
      case "scala.AnyRef" =>
        runtimeObjectClassName
      case _ =>
        tpe match {
          case TypeRef(pre, sym, _) =>
            sym.fullName.toString
        }

      // if (tpe =:= typeOf[Unit] || tpe =:= typeOf[Nothing])
      //   "void" // TODO: check
      // else if (tpe =:= typeOf[Char])
      //   "string"
      // else if (tpe =:= typeOf[Boolean])
      //   "boolean"
      // else if (tpe <:< typeOf[AnyVal])
      //   "number"
      // else if (tpe =:= typeOf[java.lang.Character] || tpe =:= typeOf[String])
      //   "?string"
      // else if (tpe =:= typeOf[java.lang.Boolean])
      //   "?boolean"
      // else if (tpe <:< typeOf[java.lang.Number])
      //   "?number"
      // else if (tpe.typeSymbol.fullName.toString.matches("""scala\.Function\d+"""))
      //   "Function"
      // else if (tpe <:< typeOf[Array[_]])
      //   "Array.<" +
      //   (tpe match {
      //     case TypeRef(_, _, List(elementTpe)) =>
      //       eraseType(elementTpe)
      //   }) +
      //   ">"
      // else if (tpe =:= typeOf[AnyRef])
      //   "Object"
      // else
      //   tpe match {
      //     case TypeRef(pre, sym, _) =>
      //       sym.fullName.toString
      //   }
    }).getOrElse("*")
  }
}
