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
  val global: Global,
  runAfter: String = "parser")
    extends PluginComponent {
  import global._
  import definitions._
  import Flags._

  override val phaseName = ExplicitTypeAnnotationsComponent.phaseName

  override val runsRightAfter = Option(runAfter)
  override val runsAfter = runsRightAfter.toList
  override val runsBefore = List("typer")

  private object N {
    def unapply(n: Name): Option[String] =
      if (n == null)
        None
      else
        Some(decode(n.toString))
  }

  private object TrivialCollectionName {
    var rx = "List|Array|Set|Seq|Iterable|Traversable".r
    def unapply(n: Name): Boolean = n.toString match {
      case rx() => true
      case _ => false
    }
  }

  private val StringTpe = typeOf[String]

  override def newPhase(prev: Phase) = new StdPhase(prev) {
    def apply(unit: CompilationUnit) {
      new Traverser {

        def getTrivialTypeTree(tree: Tree): Option[Tree] = tree match {
          /** Constant literals are the obvious case of trivial type. */
          case Literal(c @ Constant(_)) =>
            Some(TypeTree(c.tpe))

          /** String interpolation have a trivial String type. */
          case Apply(Select(Apply(Ident(N("StringContext")), _), N("s")), _) =>
            Some(TypeTree(StringTpe))

          /** Collection-constructing expression like Array(a, b...) with a, b... trivially typed has a trivial type. */
          case Apply(colTpt @ Ident(TrivialCollectionName()), args) =>
            args.map(getTrivialTypeTree(_)).map(t => (t.toString -> t)).toMap.toList match {
              // Don't accept nesting of collections as trivial: the component type must not have type params.
              case List((str, Some(componentTpt))) if !str.contains("[") =>
                Some(TypeApply(colTpt, List(componentTpt)))

              case _ =>
                None
            }

          /**
           * Collection-constructing expressions like Array[Int](...) or Map[A, B](...) have
           * a trivial type.
           */
          case Apply(ta @ TypeApply(Ident(TrivialCollectionName() | N("Map")), tparams), _) =>
            Some(ta)

          /**
           * String + Any has a trivial type.
           * Homogeneous multiplications have a trivial type.
           */
          case Apply(Select(left, N(op @ ("+" | "*"))), List(right)) =>
            (getTrivialTypeTree(left), getTrivialTypeTree(right)) match {
              case (Some(leftTpe), Some(rightTpe)) if leftTpe.toString == rightTpe.toString || (op == "+" && leftTpe.toString == "String") =>
                Some(leftTpe)
              case _ =>
                None
            }

          case _ =>
            None
        }

        def checkTypeTree(d: ValOrDefDef) {
          if (d.tpt.pos != NoPosition &&
            d.tpt.pos == d.pos &&
            d.name != nme.CONSTRUCTOR &&
            d.mods.hasNoFlags(PRIVATE | PROTECTED | SYNTHETIC | OVERRIDE | PARAM)) {

            val pos = if (d.pos == NoPosition) d.rhs.pos else d.pos
            getTrivialTypeTree(d.rhs) match {
              case Some(tpt) =>
              // reporter.info(
              //   pos,
              //   s"Extracted trivial type $tpt", force = true)

              case _ =>
                reporter.warning(
                  pos,
                  s"Public member `${d.name}` with non-trivial value should have an explicit type annotation")
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
