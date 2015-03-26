package scalaxy.react

import scala.scalajs.js
import org.scalajs.dom
import js.annotation._


// http://www.scala-js.org/doc/calling-javascript.html
// http://www.scala-js.org/doc/js-interoperability.html
// http://www.scala-js.org/doc/export-to-javascript.html
// @JSExportAll
// 
case class TimerState(secondsElapsed: Int = 0) //extends State
// trait TimerStateP extends js.Object {
//   def secondsElapsed: Int = js.native
//   def secondsElapsed_=(secondsElapsed: Int): Unit
// }

object Pickler {
  def pickle(o: Any): js.Any = {
    val state = o.asInstanceOf[TimerState]
    js.Dynamic.literal("secondsElapsed" -> state.secondsElapsed)
  }
  
  def unpickle(o: js.Any): Any = {
    val state = o.asInstanceOf[js.Dynamic]
    val secondsElapsed = state.secondsElapsed$1
    TimerState(secondsElapsed = secondsElapsed.asInstanceOf[Int])
  }
}
// @JSExport// @JSExportAll
case class Timer() extends Component[TimerState] {
  var interval: Int = _

  def tick {
    dom.console.log("tick: state = " + state)
    val copy = state.copy(secondsElapsed = state.secondsElapsed + 1)
    state = copy
  }

  override def render: ReactElement = {
    dom.console.log("render: state = " + state)
    //<div>Seconds Elapsed: {secondsElapsed}</div>
    // js.Dynamic.literal(foo = "bar")
    React.createElement("div", null, "Yeah!", state.secondsElapsed)
  }

  override def componentDidMount {
    interval = dom.setInterval(() => tick, 1000)
  }
  override def componentWillUnmount {
    dom.clearInterval(interval)
  }
}

// case class Timer() extends Component {
//   @JSExport var secondsElapsed: Int = 0
//   var interval: Int = _

//   def tick {
//     println("Ticking!")
//     secondsElapsed += 1
//   }

//   override def render: ReactElement = {
//     //<div>Seconds Elapsed: {secondsElapsed}</div>
//     // js.Dynamic.literal(foo = "bar")
//     React.createElement("div", null, "Yeah!", secondsElapsed)
//   }

//   override def componentDidMount {
//     println("componentDidMount!")
//     interval = dom.setInterval(() => tick, 1000)
//   }
//   override def componentWillUnmount {
//     println("componentWillUnmount!")
//     dom.clearInterval(interval)
//   }
// }

