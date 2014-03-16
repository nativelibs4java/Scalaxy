package scalaxy.streams

private[streams] trait Utils {
  val global: scala.reflect.api.Universe
  import global._

  lazy val EmptyName: TermName = ""

  trait Extractor[From, To] {
    def unapply(from: From): Option[To]
  }

  implicit class SymbolExtensions(s: Symbol) {
    def asOption: Option[Symbol] =
      if (s == NoSymbol) None else Some(s)
  }
  implicit class TreeExtensions(t: Tree) {
    def asOption: Option[Tree] =
      if (t == EmptyTree) None else Some(t)
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

  private lazy val defaultValues = Map[Type, Any](
    typeOf[Int] -> 0,
    typeOf[Boolean] -> false,
    typeOf[Byte] -> (0: Byte),
    typeOf[Short] -> (0: Short),
    typeOf[Char] -> '\0',
    typeOf[Long] -> 0L,
    typeOf[Float] -> 0.0f,
    typeOf[Double] -> 0.0)

  def defaultValue(tpe: Type): Any =
    defaultValues.get(tpe.normalize).getOrElse(null)
}
