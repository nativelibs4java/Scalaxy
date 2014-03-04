package scalaxy.loops

private[loops] trait Utils {
  val global: scala.reflect.api.Universe
  import global._

  lazy val EmptyName: TermName = ""

  def trySome[T](v: => T): Option[T] =
    try {
      Some(v)
    } catch { case ex: Throwable =>
      ex.printStackTrace
      None
    }

}
