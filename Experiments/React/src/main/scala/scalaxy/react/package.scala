package scalaxy

import scala.reflect.ClassTag

import org.scalajs.dom
import scala.scalajs.js
import js.annotation._

package react {

  @JSExportDescendentObjects
  trait Component[S] {
    @JSExport
    def render: ReactElement

    private[react] var that: ReactComponentThis = _

    def state: S = {
      // PicklerRegistry.unpickle(that.state).asInstanceOf[S]
      // that.state.asInstanceOf[S]
      Pickler.unpickle(that.state).asInstanceOf[S]
    }
    def state_=(newState: S): Unit = {
      // TODO: macro w/ copy-awareness to dispatch to replaceState or setState
      //that.setState(PicklerRegistry.pickle(newState))
      val pickled = Pickler.pickle(newState)
      that.setState(pickled)
      // that.setState(newState)
    }

    @JSExport
    def componentDidMount: Unit
    @JSExport
    def componentWillUnmount: Unit
  }

}

package object react {
  // type RenderOutput = js.Object

  // private[react] var currentReactComponentThis: ReactComponentThis = null
  
  // private[this] def withCurrent[A](that: ReactComponentThis)
  //                                 (block: => A): A = {
  //   val prev = currentReactComponentThis
  //   try {
  //     currentReactComponentThis = that
  //     block
  //   } finally {
  //     currentReactComponentThis = prev
  //   }
  // }

  //def parse(text: String): js.Any = js.native
  implicit def xmlNode2Object(node: scala.xml.Node): js.Object = ???
  implicit def xmlNodeSeq2Object(node: scala.xml.NodeSeq): js.Object = ???

  private[this] val componentCache = collection.mutable.Map[ClassTag[_], ReactClass]()

  def createElement[A <: Component[_] : ClassTag]: ReactElement =
    React.createElement(componentCache(implicitly[ClassTag[A]]), null)

  private[this] def withComp[C <: Component[_], R]
                            (f: C => R, newInstance: => C)
                            : js.ThisFunction0[ReactComponentThis, R] =
    (that: ReactComponentThis) => {//withCurrent(that) {
      var c = that.instance
      if (c == {}) {
      //if (!c.isInstanceOf[C]) {
        val instance = newInstance
        instance.that = that
        that.instance = instance
        c = instance
      }
      f(c.asInstanceOf[C])
      //f(that.state.asInstanceOf[C])
    }

  def createClass[S, C <: Component[S] : ClassTag]
                 (newState: => S, newInstance: => C)
                 : ReactClass = {
  // def createClass[C <: Component : ClassTag](newInstance: () => C): ReactClass = {
    // val render: js.ThisFunction0[ReactComponentThis, ReactElement] =
    //   (that: ReactComponentThis) => withCurrent(that) {
    //     val c = that.state.asInstanceOf[C]
    //     print(c)
    //     c.render
    //   }

    val cls = React.createClass(
      js.Dynamic.literal(
        "getInitialState" -> (() => newState),
        "render" -> withComp((_: C).render, newInstance),
        "componentDidMount" -> withComp((_: C).componentDidMount, newInstance),
        "componentWillUnmount" -> withComp((_: C).componentWillUnmount, newInstance)
      )
    )
    componentCache(implicitly[ClassTag[C]]) = cls
    cls
  }
}
