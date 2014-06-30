package scalaxy

import scala.language.experimental.macros

import scala.reflect.macros.blackbox.Context
import scala.collection.mutable

/**
 * Perform extra compilation-time checks.
 * Checks:
 * - Confusing names in case class extractors
 * - Ambiguous unnamed arguments with same type
 * - Confusing names in method calls
 * - (TODO) Potential side-effect free statements (e.g. missing + between multiline concatenations)
 */
package object parano {
  /**
   * Perform parano checks in the enclosing compilation unit.
   */
  def verify() = macro impl

  /** getFragments("this_isTheEnd") == Array("this", "is", "The", "End") */
  private def getFragments(name: String) =
    name.split("""\b|[_-]|(?<=[a-z])(?=[A-Z])""").filter(_.length >= 3)

  private case class RichName(name: String) {
    val fragments = getFragments(name).toSet

    def containsExactFragment(str: String): Boolean = {
      name.contains(str) ||
        fragments.exists(_.equalsIgnoreCase(str)) ||
        getFragments(str).exists(frag => fragments.exists(_.equalsIgnoreCase(frag)))
    }
  }

  def impl(c: Context)(): c.Expr[Unit] =
    {
      val tree = c.typecheck(c.enclosingUnit.body, withMacrosDisabled = true)

      class MacroParanoChecks(c: Context) extends ParanoChecks {
        override val global = c.universe
        import global._

        override def error(pos: Position, msg: String) =
          c.error(pos.asInstanceOf[c.universe.Position], msg)

        // TODO: Flag.SYNTHETIC does not exist in Scala 2.10.2, use it when available.
        override def isSynthetic(mods: Modifiers) =
          mods.toString.contains("<synthetic>")
        //mods.hasFlag(Flag.SYNTHETIC)

        check(tree.asInstanceOf[Tree])
      }
      new MacroParanoChecks(c)
      //println(tree)
      //println(showRaw(tree))

      import c.universe._
      c.Expr(q"()")
    }
}
