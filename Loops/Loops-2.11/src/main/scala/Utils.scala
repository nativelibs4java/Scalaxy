package scalaxy.loops

private[loops] trait Utils {
  val global: scala.reflect.api.Universe
  import global._

  lazy val EmptyName: TermName = ""

  implicit class SymbolExtensions(s: Symbol) {
    def asOption: Option[Symbol] =
      if (s == NoSymbol) None else Some(s)
  }
  implicit class NameExtensions(n: TermName) {
    def asOption: Option[TermName] =
      if (n.toString == "") None else Some(n)
  }

  def trySome[T](v: => T): Option[T] =
    try {
      Some(v)
    } catch { case ex: Throwable =>
      ex.printStackTrace
      None
    }

}
