package scalaxy.streams

import scala.annotation.tailrec

trait Blacklists extends Reporters {
  val global: scala.reflect.api.Universe
  import global._

  private[this] val SCALAXY_STREAMS_SKIP_ENV_NAME = "SCALAXY_STREAMS_SKIP"

  private[this] val SCALAXY_STREAMS_SKIP_PROPERTY_NAME = "scalaxy.streams.skip"

  /** fileName:symbolName:line */
  private[this] val patterns =
    Option(System.getenv(SCALAXY_STREAMS_SKIP_ENV_NAME))
      .getOrElse(System.getProperty(SCALAXY_STREAMS_SKIP_PROPERTY_NAME, ""))
      .split(",")
      .map(_.split(":")) map {
    case Array(name, symbol, lineStr) =>
      (name, Some(symbol), Some(lineStr.toInt))
    case Array(name, symbolOrLine) =>
      try {
        (name, None, Some(symbolOrLine.toInt))
      } catch { case ex: Throwable =>
        (name, Some(symbolOrLine), None)
      }
    case Array(name) =>
      (name, None, None)
  }

  @tailrec
  private[this] def enclosingSymbol(sym: Symbol): Option[Symbol] = {
    def isNamed(tsym: TermSymbol) =
      tsym.isMethod || tsym.isVal || tsym.isModule

    if (sym == NoSymbol) {
      None
    } else if (!sym.isSynthetic &&
               sym.isTerm && isNamed(sym.asTerm) ||
               sym.isType && sym.asType.isClass) {
      Some(sym)
    } else {
      enclosingSymbol(sym.owner)
    }
  }

  def isBlacklisted(pos: Position, currentOwner: Symbol): Boolean = {
    // TODO: optimize this (indexing patterns by file name).
    val fileName = pos.source.file.path.split("/").last
    lazy val enclosingSymbolName = enclosingSymbol(currentOwner).map(_.name.toString)
    val line: Int = pos.line
    patterns.exists({
      case (name, symbolOpt, lineOpt) if name == fileName =>
        symbolOpt.forall(s => enclosingSymbolName.contains(s)) &&
        lineOpt.forall(_ == line)

      case _ =>
        false
    })
  }
}
