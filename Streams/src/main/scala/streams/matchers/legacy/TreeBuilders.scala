package scalaxy.streams

import scala.reflect.api.Universe
import scala.collection.mutable.ArrayBuffer

/**
 * Refugee from Scalaxy/Components
 * TODO: modernize (quasiquotes...) / make it go away.
 */
trait TreeBuilders
    extends WithLocalContext {
  val global: Universe
  import global._
  import global.definitions._

  type TreeGen = () => Tree
  type IdentGen = () => Ident

  case class ValueDef(rawIdentGen: IdentGen, definition: ValDef, tpe: Type) {
    var identUsed = false
    val identGen: IdentGen = () => {
      identUsed = true
      rawIdentGen()
    }
    def apply() = identGen()

    def defIfUsed = ifUsed(definition)
    def ifUsed[V](v: => V) = if (identUsed) Some(v) else None
  }

  def newVal(prefix: String, value: Tree, tpe: Type) =
    newValueDef(prefix, false, value, tpe)

  def newVar(prefix: String, value: Tree, tpe: Type) =
    newValueDef(prefix, true, value, tpe)

  private def newValueDef(prefix: String, mutable: Boolean, value: Tree, tpe: Type) = {
    val vd = ValDef(
      if (mutable) Modifiers(Flag.MUTABLE) else NoMods,
      TermName(fresh(prefix)),
      TypeTree(tpe),
      value)
    ValueDef(() => Ident(vd.name), vd, tpe)
  }
}
