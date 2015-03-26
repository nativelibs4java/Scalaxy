package scalaxy.react
package test

import org.scalajs.dom
import scala.scalajs.js

import jsx._

// import org.scalajs.spickling.jsany._
// import org.scalajs.spickling._

// http://scala-js.github.io/scala-js-dom/
class JSXTest {
  def test {

    val props = Map("x" -> 10, "y" -> 12)
    val link = "http://goog.gl"

    val component: ReactComponent = null

    jsx"""
      <div $props>
        Yeah baby
        <a href="$link" />
        <$component>
        </$component>
      </div>
    """
  }
}
