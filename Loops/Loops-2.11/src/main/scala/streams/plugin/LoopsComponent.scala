// Author: Olivier Chafik (http://ochafik.com)
package scalaxy.loops

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
 *  > val Some(List(ast)) = intp.parse("@public def str = self.toString")
 *  > nodeToString(ast)
 *  > val DefDef(mods, name, tparams, vparamss, tpt, rhs) = ast // play with extractors to explore the tree and its properties.
 */
object LoopsComponent {
  val phaseName = "scalaxy-loops"
}
class LoopsComponent(
  val global: Global, runAfter: String = "typer")
    extends PluginComponent
    with Streams {
  import global._
  import definitions._
  import Flags._

  override val phaseName = LoopsComponent.phaseName

  override val runsRightAfter = Option(runAfter)
  override val runsAfter = runsRightAfter.toList
  override val runsBefore = List("patmat")

  override def typed(tree: Tree, tpe: Type) = {
    try {
      typer.typed(tree, tpe)
    } catch { case ex: Throwable =>
      throw new RuntimeException(tree.toString, ex)
    }
  }

  override def newPhase(prev: Phase) = new StdPhase(prev) {
    def apply(unit: CompilationUnit) {
      unit.body = new Transformer {

        override def transform(tree: Tree) = tree match {
          case SomeStream(stream) =>
            reporter.info(tree.pos, impl.optimizedStreamMessage(stream.describe()), force = true)
            val result = typer.typed {
              stream.emitStream(n => unit.fresh.newName(n): TermName, transform(_)).compose
            }
            // println(tree)
            // println(result)

            result

          case _ =>
            super.transform(tree)
        }
      } transform unit.body
    }
  }
}
