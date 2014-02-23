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
class ExplicitTypeAnnotations(
  val global: Global)
    extends PluginComponent {
  import global._
  import definitions._
  import Flags._

  override val phaseName = "scalaxy-explicit-annotations"

  override val runsRightAfter = Option("refchecks")
  override val runsAfter = runsRightAfter.toList

  object N {
    def unapply(n: Name): Option[String] = Option(n.toString).map(s => decode(s))
  }

  override def newPhase(prev: Phase) = new StdPhase(prev) {
    def apply(unit: CompilationUnit) {
      new Traverser {

        def isTrivialRHS(rhs: Tree): Boolean = rhs match {
          case Literal(Constant(_)) =>
            true

          case Apply(Select(sc, N("s")), _) if typeOf[StringContext] =:= sc.tpe =>
            true

          case Apply(Select(left, N("+" | "*")), List(right)) if left.tpe =:= right.tpe =>
            isTrivialRHS(left) && isTrivialRHS(rhs)

          case _ =>
            false
        }
        def checkTypeTree(d: ValOrDefDef) {
          if (d.rhs.pos != NoPosition &&
            d.name != nme.CONSTRUCTOR &&
            d.mods.hasNoFlags(PRIVATE | PROTECTED | SYNTHETIC | OVERRIDE) &&
            !isTrivialRHS(d.rhs)) {

            if ((d.tpt.pos == NoPosition || d.tpt.pos == d.rhs.pos || d.tpt.pos == d.pos) &&
              !(typeOf[Unit] =:= d.rhs.tpe)) {

              reporter.warning(
                if (d.pos == NoPosition) d.rhs.pos else d.pos,
                "Public members with non-trivial bodies should have explicit type annotations")
            }
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
