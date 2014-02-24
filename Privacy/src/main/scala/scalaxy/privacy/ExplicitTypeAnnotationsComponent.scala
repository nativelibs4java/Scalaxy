// Author: Olivier Chafik (http://ochafik.com)
package scalaxy.privacy

import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.symtab.Flags

import scala.reflect.NameTransformer.{ encode, decode }

/**
 *  Makes sure type annotations are explicitly set on all public members with non-trivial bodies, for readability purposes.
 */
object ExplicitTypeAnnotationsComponent {
  val phaseName = "scalaxy-explicit-annotations"
}
class ExplicitTypeAnnotationsComponent(
  val global: Global, runAfter: String = "parser")
    extends PluginComponent {
  import global._
  import definitions._
  import Flags._

  override val phaseName = ExplicitTypeAnnotationsComponent.phaseName

  override val runsRightAfter = Option(runAfter)
  override val runsAfter = runsRightAfter.toList
  override val runsBefore = List("typer")

  object N {
    def unapply(n: Name): Option[String] =
      if (n == null)
        None
      else
        Some(decode(n.toString))
  }

  override def newPhase(prev: Phase) = new StdPhase(prev) {
    def apply(unit: CompilationUnit) {
      new Traverser {

        def isTrivialRHS(rhs: Tree): Boolean = rhs match {
          case Literal(Constant(_)) =>
            true

          case Apply(Select(Apply(Ident(N("StringContext")), _), N("s")), _) =>
            true

          case Apply(Select(left, N("+" | "*")), List(right)) =>
            isTrivialRHS(left) && isTrivialRHS(right)

          case _ =>
            false
        }
        def checkTypeTree(d: ValOrDefDef) {
          // reporter.info(d.pos, "d.pos", force = true)
          // reporter.info(d.tpt.pos, "d.tpt.pos (" + d.tpt.getClass.getName + ")", force = true)
          // reporter.info(d.rhs.pos, "d.rhs.pos", force = true)

          if (d.tpt.pos != NoPosition &&
            d.tpt.pos == d.pos &&
            d.name != nme.CONSTRUCTOR &&
            d.mods.hasNoFlags(PRIVATE | PROTECTED | SYNTHETIC | OVERRIDE) &&
            !isTrivialRHS(d.rhs)) {

            reporter.warning(
              if (d.pos == NoPosition) d.rhs.pos else d.pos,
              s"Public member `${d.name}` with non-trivial value should have a explicit type annotation")
          }
        }

        override def traverse(tree: Tree) = {
          tree match {
            case d @ ValDef(mods, name, tpt, rhs) =>
              checkTypeTree(d)

            case d @ DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
              checkTypeTree(d)

            case _ =>
          }
          super.traverse(tree)
        }
      } traverse unit.body
    }
  }
}
