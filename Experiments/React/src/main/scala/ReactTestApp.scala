package scalaxy.react

import org.scalajs.dom
import scala.scalajs.js

import jsx._

// import org.scalajs.spickling.jsany._
// import org.scalajs.spickling._

// http://scala-js.github.io/scala-js-dom/
object ReactTestApp extends js.JSApp {
  def main(): Unit = {
    println("Hello world!")

    //val reg = registerComponent[Timer](new Timer)
    // 
    // PicklerRegistry.register[TimerState]
    
    val reg = createClass(TimerState(), Timer())
    println(reg)

    val elt = createElement[Timer]
    println(elt)


    val node = dom.document.getElementById("container").asInstanceOf[dom.Node]
    React.render(elt, node)
    //React.render(React.createElement(Timer, null), mountNode);
  }
}
