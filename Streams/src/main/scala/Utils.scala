package scalaxy.streams

import scala.language.postfixOps
import scala.language.implicitConversions

//private[streams]
trait Utils {
  val global: scala.reflect.api.Universe
  import global._
  import definitions._

  private[streams] lazy val EmptyName = TermName("")

  trait Extractor[From, To] {
    def unapply(from: From): Option[To]
  }

  // private[streams] 
  object S {
    def unapply(symbol: Symbol) = Option(symbol).map(_.name.toString)
  }

  class N(val s: String) {
    def unapply(n: Name): Boolean = n.toString == s
    def apply() = TermName(s)
  }
  object N {
    def apply(s: String) = new N(s)
    def unapply(n: Name): Option[String] = Option(n).map(_.toString)
  }
  implicit def N2TermName(n: N) = n()

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

  private[streams] def trySome[T](v: => T): Option[T] =
    try {
      Some(v)
    } catch { case ex: Throwable =>
      if (impl.debug)
        ex.printStackTrace()
      None
    }

  private[streams] def tryOrNone[T](v: => Option[T]): Option[T] =
    try {
      v
    } catch { case ex: Throwable =>
      if (impl.debug)
        ex.printStackTrace()
      None
    }

  private[streams] def dummyStatement(fresh: String => TermName) =
    q"val ${fresh("dummy")} = null"

  // Option(tpe).map(_.dealias.widen).orNull
  // private[streams]
  def normalize(tpe: Type): Type = Option(tpe).map(_.dealias) collect {
    case t @ SingleType(_, _) =>
      t.widen
    case t @ ConstantType(_) =>
      /// There's no `deconst` in the api (only in internal). Work around it:
      t.typeSymbol.asType.toType
    case t =>
      t
  } orNull

  private[streams] def findType(tree: Tree): Option[Type] =
    Option(tree.tpe).orElse(Option(tree.symbol).map(_.typeSignature))
      .filter(_ != NoType)
      .map(normalize)

  def getDefaultValue(tpe: Type): Any = normalize(tpe) match {
    case IntTpe => 0
    case BooleanTpe => false
    case ByteTpe => 0: Byte
    case ShortTpe => 0: Short
    case CharTpe => '\u0000'
    case LongTpe => 0L
    case FloatTpe => 0.0f
    case DoubleTpe => 0.0
    case s => null
  }

  private[streams] def newVar(name: TermName, tpe: Type, rhs: Tree = EmptyTree): ValDef = {
    val ntpe = normalize(tpe)
    val initialValue = rhs.orElse(Literal(Constant(getDefaultValue(ntpe))))
    // val initialValue = rhs match {
    //   case EmptyTree =>
    //     Literal(Constant(getDefaultValue(ntpe)))
    //     // q"null.asInstanceOf[$ntpe]"
    //   case _ =>
    //     rhs
    // }
    // Note: this looks weird, but it does give 0 for Int :-).
    q"private[this] var $name: $ntpe = $initialValue"
  }
}
