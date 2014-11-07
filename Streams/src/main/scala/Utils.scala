package scalaxy.streams

import scala.language.postfixOps

private[streams] trait Utils {
  val global: scala.reflect.api.Universe
  import global._

  lazy val EmptyName = TermName("")

  trait Extractor[From, To] {
    def unapply(from: From): Option[To]
  }

  object S {
    def unapply(symbol: Symbol) = Option(symbol).map(_.name.toString)
  }

  object N {
    def unapply(name: Name) = Option(name).map(_.toString)
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
      if (impl.debug)
        ex.printStackTrace()
      None
    }

  def tryOrNone[T](v: => Option[T]): Option[T] =
    try {
      v
    } catch { case ex: Throwable =>
      if (impl.debug)
        ex.printStackTrace()
      None
    }

  def dummyStatement(fresh: String => TermName) =
    q"val ${fresh("dummy")} = null"

  def normalize(tpe: Type): Type = Option(tpe).map(_.dealias) collect {
    case t @ SingleType(_, _) =>
      t.widen
    case t @ ConstantType(_) =>
      /// There's no `deconst` in the api (only in internal). Work around it:
      t.typeSymbol.asType.toType
    case t =>
      t
  } orNull

  def findType(tree: Tree): Option[Type] =
    Option(tree.tpe).orElse(Option(tree.symbol).map(_.typeSignature))
      .filter(_ != NoType)
      .map(normalize)

  def newVar(name: TermName, tpe: Type, rhs: Tree = EmptyTree): ValDef = {
    val ntpe = normalize(tpe)
    val initialValue = rhs match {
      case EmptyTree =>
        q"null.asInstanceOf[$ntpe]"
      case _ =>
        rhs
    }
    // Note: this looks weird, but it does give 0 for Int :-).
    q"private[this] var $name: $ntpe = $initialValue"
  }
}
