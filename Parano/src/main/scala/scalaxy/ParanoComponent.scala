// Author: Olivier Chafik (http://ochafik.com)
package scalaxy.parano

import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.symtab.Flags

/**
 *  To understand / reproduce this, you should use paulp's :power mode in the scala console:
 *
 *  scala
 *  > :power
 *  > :phase parser // will show us ASTs just after parsing
 *  > val Some(List(ast)) = intp.parse("@scalaxy.extension[Int] def str = self.toString")
 *  > nodeToString(ast)
 *  > val DefDef(mods, name, tparams, vparamss, tpt, rhs) = ast // play with extractors to explore the tree and its properties.
 */
class ParanoComponent(
  val global: Global)
    extends PluginComponent
    with ParanoChecks {
  import global._
  import definitions._

  override val phaseName = "scalaxy-parano"

  override val runsAfter = List("typer")
  override val runsBefore = List("patmat")

  override def info(pos: Position, msg: String, force: Boolean) = reporter.info(pos, msg, force)
  override def error(pos: Position, msg: String) = reporter.error(pos, msg)

  override def isSynthetic(mods: Modifiers) =
    mods.hasFlag(Flags.SYNTHETIC)

  def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {
    def apply(unit: CompilationUnit) {
      check(unit.body)
    }
  }
}
