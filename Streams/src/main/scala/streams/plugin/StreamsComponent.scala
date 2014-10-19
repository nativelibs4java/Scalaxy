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
object StreamsComponent {
  val phaseName = "scalaxy-streams"
}
class StreamsComponent(
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

  // lazy val OptimizationStrategyTpe = rootMirror.staticClass("scalaxy.streams.OptimizationStrategy")

  override def newPhase(prev: Phase) = new StdPhase(prev) {
    def apply(unit: CompilationUnit) {
      if (!impl.disabled) {
        val transformer = new TypingTransformer(unit) {

          // val typed: Tree => Tree = localTyper.typed(_)
          private[this] val typed: Tree => Tree =
            (tree: Tree) => try {
              localTyper.typed(tree)
            } catch { case ex: Throwable =>
              throw new RuntimeException("Failed to type " + tree + "\n(" + ex + ")", ex)
            }

          private[this] val untyped: Tree => Tree =
            (tree: Tree) => try {
              resetAttrs(tree)
              // localTyper.untypecheck(tree)
            } catch { case ex: Throwable =>
              throw new RuntimeException("Failed to untype " + tree, ex)
            }

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

          override def transform(tree: Tree) = tree match {
            case SomeStream(stream) =>
              val strategy = getStrategy(tree.pos)
              if (stream.isWorthOptimizing(strategy, reporter.info(_, _, force = true), reporter.warning)) {

                reporter.info(
                  tree.pos,
                  Optimizations.optimizedStreamMessage(stream.describe()),
                  force = impl.verbose)

                try {
                  val result = {
                    stream
                      .emitStream(
                        n => TermName(unit.fresh.newName(n)),
                        transform(_),
                        currentOwner = currentOwner,
                        typed = typed,
                        untyped = untyped)
                      .compose(localTyper.typed(_))
                  }

                  if (impl.veryVerbose) {
                    reporter.info(
                      tree.pos,
                      s"${Optimizations.messageHeader}Result for ${stream.describe()}:\n$result",
                      force = impl.verbose)
                  }
                  // println(result)

                  // def resetLocalAttrs(tree: Tree): Tree =
                  //   resetAttrs(duplicateAndKeepPositions(tree))
                  //
                  // def untypecheck(tree: Tree): Tree =
                  //   resetLocalAttrs(tree)
                  // 
                  // typed(untypecheck(result))
                  result
                } catch {
                  case ex: Throwable =>
                    ex.printStackTrace()
                    super.transform(tree)
                }
              } else {
                if (impl.veryVerbose) {
                  reporter.info(
                    tree.pos,
                    s"${Optimizations.messageHeader}Stream ${stream.describe()} is not worth optimizing with strategy $strategy",
                    force = impl.verbose)
                }
                super.transform(tree)
              }

            case _ =>
              super.transform(tree)
          }
        }

        unit.body = transformer transform unit.body
      }
    }
  }
}
