package scalaxy; package components

import scala.reflect._
import Function.tupled

object UniverseConversions
{
  def convert[F <: base.Universe, T <: base.Universe](from: F, to: T)(
    tree: from.Tree, 
    nameBindings: Map[from.Symbol, to.Tree] = Map(),
    typeBindings: Map[from.Type, to.Type] = Map()
  ): to.Tree = {
    null
  }
}
