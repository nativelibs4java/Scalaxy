package scalaxy.js

import scala.language.dynamics

class DynamicValue(value: Any) extends Dynamic {
  ???
  def applyDynamic(name: String)(args: Any*): Any = ???
  def selectDynamic(name: String): Any = ???
  def updateDynamic(name: String)(value: Any) = ???
}

