// Author: Olivier Chafik (http://ochafik.com)
package scalaxy.fastcaseclasses

import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.symtab.Flags

class UntypedFastCaseClassesComponent(val global: Global)
    extends PluginComponent
    with UntypedFastCaseClassesTransforms {
  import global._

  override val phaseName = "scalaxy-fastcaseclasses"
  override val runsAfter = List("parser")
  override val runsBefore = List("namer")

  override def info(pos: Position, msg: String) = reporter.info(pos, msg, force = verbose)

  def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {
    def apply(unit: CompilationUnit) {
      unit.body = transformUntyped(unit.body, unit.fresh.newName(_))
    }
  }
}
