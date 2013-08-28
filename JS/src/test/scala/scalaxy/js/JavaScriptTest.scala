package scalaxy.js

import Global._

@JavaScript
object JavaScriptTest {
  println("hello, world")
  println("hello, world, again")
  window.asDynamic.alert("Yeah, babe")
  window.alert("I'm here")
  lazy val callMeToSeeWhatHappens = {
    println("You've called me!")
    10
  }

  val msg = "Bonjour !"
  js"alert($msg)"

  val someVal = "fooo"
}
