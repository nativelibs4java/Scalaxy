package scalaxy.streams
package test

trait TestReporters extends Reporters
{
  val global: scala.reflect.api.Universe
  import global._

  def log(level: String, pos: Position, msg: String) {
    println(s"[$level] $msg ($pos)")
  }
  override def info(pos: Position, msg: String, force: Boolean) {
    log("info", pos, msg)
  }
  override def warning(pos: Position, msg: String) {
    log("warning", pos, msg)
  }
  override def error(pos: Position, msg: String) {
    log("error", pos, msg)
  }
}
