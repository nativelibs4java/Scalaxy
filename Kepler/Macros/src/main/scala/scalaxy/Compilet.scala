package scalaxy

//package macros

import scala.reflect.mirror._

trait Compilet {
  def matchActions: Seq[(String, MatchAction)]
}
