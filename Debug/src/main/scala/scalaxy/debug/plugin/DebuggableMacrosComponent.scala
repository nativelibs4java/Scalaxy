// Author: Olivier Chafik (http://ochafik.com)
package scalaxy.debug.plugin
 
import scala.reflect.internal._
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.TypingTransformers
 
class DebuggableMacrosComponent(val global: Global)
    extends PluginComponent
    with TypingTransformers
{
  import global._
  import definitions._
 
  override val phaseName = "scalaxy-debuggable-macros"
 
  override val runsRightAfter = Some("typer")
  override val runsAfter = runsRightAfter.toList
  override val runsBefore = List[String]("refchecks")
 
  def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {
    def apply(unit: CompilationUnit) {
      var hasUnannotatedTrees = false 
      val unannotatedTreesDetector = new Traverser(unit) {
        override def traverse(tree: Tree) {
          if (tree.pos == NoPosition) {
            println("Unannotated tree: " + tree)
            hasUnannotatedTrees = true
          }
          super.traverse(tree)
        }
      }
      unit.body = unannotatedTreesDetector.traverse(unit.body)
    }
  }
}
