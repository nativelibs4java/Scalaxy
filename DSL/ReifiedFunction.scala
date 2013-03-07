package scalaxy.dsl

import scala.reflect.runtime.universe.{ Symbol, Function }

case class ReifiedFunction[A, B](
  function: A => B,
  captures: Map[String, () => Any], 
  tree: Function)

