package scalaxy.js

import scala.language.implicitConversions

sealed trait OptionalParam[+T] {
  implicit def asOption: Option[T]
}
case object NoParam extends OptionalParam[Nothing] {
  override def asOption = None
}
case class SomeParam[+T](value: T) extends OptionalParam[T] {
  override def asOption = Some(value)
}
object OptionalParam {
  implicit def someOptionalParam[T](value: T): OptionalParam[T] = SomeParam[T](value)
}
