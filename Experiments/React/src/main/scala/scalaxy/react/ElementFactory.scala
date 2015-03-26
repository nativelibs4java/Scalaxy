package scalaxy.react

import org.scalajs.dom
import scala.scalajs.js
import js.annotation._

trait ElementFactory[E, C] extends js.Any {
  def createElement(name: String, props: js.Object, children: Any*): E = js.native
  def createElement(cls: C, props: js.Object, children: Any*): E = js.native
}
