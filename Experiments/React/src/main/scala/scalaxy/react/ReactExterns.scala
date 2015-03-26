package scalaxy.react

import org.scalajs.dom
import scala.scalajs.js
import js.annotation._

trait ReactClass extends js.Object
trait ReactElement extends js.Object
trait ReactComponent extends js.Object

trait ReactComponentThis extends js.Object {
  def instance: Any = js.native
  def instance_=(c: Any): Unit = js.native

  def replaceState(newState: js.Object): Unit = js.native
  def setState(newState: js.Any): Unit = js.native
  def state: js.Any = js.native
}

object React extends js.Object with ElementFactory[ReactElement, ReactClass] {
  def render(e: ReactElement, n: dom.Node): ReactComponent = js.native
  
  def createElement(tpe: ReactClass, props: js.Object, children: js.Any*): ReactElement = js.native
  
  def createElement(tpe: String, props: js.Object, children: js.Any*): ReactElement = js.native
  
  def createClass(spec: js.Object): ReactClass = js.native
}
