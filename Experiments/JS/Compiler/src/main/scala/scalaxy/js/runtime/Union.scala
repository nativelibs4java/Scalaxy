package scalaxy.js

import scala.language.implicitConversions

class |[A, B](val value: Any)
object | {
  implicit def union[A, B](value: Any): (A | B) = new |[A, B](value)
}

// class Union[A, B](val value: Any)
// object Union {
//   implicit def union[A, B](value: Any): Union[A, B] = new Union[A, B](value)
// }
