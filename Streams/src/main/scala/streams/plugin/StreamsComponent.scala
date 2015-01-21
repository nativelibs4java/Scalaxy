// Author: Olivier Chafik (http://ochafik.com)
package scalaxy.streams

import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.symtab.Flags
import scala.tools.nsc.transform.TypingTransformers

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
private[streams] object StreamsComponent {
  val phaseName = "scalaxy-streams"
}
private[streams] class StreamsComponent(
  val global: Global, runAfter: String = "typer")
    extends PluginComponent
    with StreamTransforms
    with TypingTransformers {
  import global._
  import definitions._
  import Flags._

  override val phaseName = StreamsComponent.phaseName

  override val runsRightAfter = Option(runAfter)
  override val runsAfter = runsRightAfter.toList
  override val runsBefore = List("patmat")

  override def info(pos: Position, msg: String, force: Boolean) {
    reporter.info(pos, msg, force = force)
  }
  override def warning(pos: Position, msg: String) {
    reporter.warning(pos, msg)
  }
  override def error(pos: Position, msg: String) {
    reporter.error(pos, msg)
  }

  override def newPhase(prev: Phase) = new StdPhase(prev) {
    def apply(unit: CompilationUnit) {
      if (!impl.disabled) {
        val transformer = new TypingTransformer(unit) {

          def typed(tree: Tree) = try {
            localTyper.typed(tree)
          } catch { case ex: Throwable =>
            throw new RuntimeException("Failed to type " + tree + "\n(" + ex + ")", ex)
          }

          // TODO: this is probably a very slow way to get the strategy :-S
          def getStrategy(pos: Position) =
            Optimizations.matchStrategyTree(global)(
              rootMirror.staticClass(_),
              tpe => analyzer.inferImplicit(
                EmptyTree,
                tpe,
                reportAmbiguous = true,
                isView = false,
                context = localTyper.context,
                saveAmbiguousDivergent = false,
                pos = pos
              ).tree
            )

          override def transform(tree: Tree) = {
            def opt(tree: Tree) = try {
              transformStream(
                tree = tree,
                strategy = getStrategy(tree.pos),
                fresh = unit.fresh.newName,
                currentOwner = currentOwner,
                recur = transform(_),
                typecheck = typed(_))
            } catch {
              case ex: Throwable =>
                logException(tree.pos, ex)
                None
            }

            opt(tree).getOrElse {
              val sup = super.transform(tree)
              opt(sup).getOrElse(sup)
            }
          }
        }

        unit.body = transformer transform unit.body
      }
    }
  }
}
