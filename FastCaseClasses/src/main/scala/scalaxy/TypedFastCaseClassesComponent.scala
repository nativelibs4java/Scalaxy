// Author: Olivier Chafik (http://ochafik.com)
package scalaxy.fastcaseclasses

import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.symtab.Flags

class TypedFastCaseClassesComponent(val global: Global)
    extends PluginComponent
    with TypedFastCaseClassesTransforms {
  import global._

  override val phaseName = "scalaxy-fastcaseclasses"
  override val runsAfter = List("typer")
  override val runsBefore = List("patmat")

  override def info(pos: Position, msg: String) = reporter.info(pos, msg, force = verbose)

  def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {
    def apply(unit: CompilationUnit) {
      val file = unit.source.file
      // println(s"#\n# Processing $file...\n#")
      unit.body = transformTyped(unit.body, unit)
      // println(s"#\n# Done with $file.\n#")
    }
  }
}
