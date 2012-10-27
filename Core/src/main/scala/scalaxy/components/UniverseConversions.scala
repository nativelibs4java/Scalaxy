package scalaxy; package components

import scala.reflect._
import Function.tupled

object UniverseConversions
{
  def convert[F <: api.Universe, T <: api.Universe](from: F, to: T)(
    tree: from.Tree,
    nameBindings: Map[from.Symbol, to.Tree] = Map(),
    typeBindings: Map[from.Type, to.Type] = Map()
  ): to.Tree = {
    null
  }
}
