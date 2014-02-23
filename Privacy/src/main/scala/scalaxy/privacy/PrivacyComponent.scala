// Author: Olivier Chafik (http://ochafik.com)
package scalaxy.privacy

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
class PrivacyComponent(
  val global: Global)
    extends PluginComponent {
  import global._
  import definitions._
  import Flags._

  override val phaseName = "scalaxy-privacy"

  override val runsRightAfter = Option("parser")
  override val runsAfter = runsRightAfter.toList
  override val runsBefore = List("namer")

  private val PublicName = "public"
  private val NoPrivacyName = "noprivacy"

  private val defaultVisibilityString = "private[this]"
  private val defaultVisibilityFlags = PRIVATE | LOCAL

  lazy val printNoPrivacyHint: Unit = {
    reporter.info(
      NoPosition,
      s"To prevent $phaseName from changing default visiblity to $defaultVisibilityString, use @$NoPrivacyName",
      force = true)
  }

  private object SimpleAnnotation {
    def unapply(ann: Tree): Option[String] = Option(ann) collect {
      case Apply(Select(New(Ident(tpt)), nme.CONSTRUCTOR), _) =>
        tpt.toString
    }
  }

  override def newPhase(prev: Phase) = new StdPhase(prev) {
    def apply(unit: CompilationUnit) {
      unit.body = new Transformer {

        def hasSimpleAnnotation(mods: Modifiers, name: String): Boolean =
          mods.annotations.exists({
            case SimpleAnnotation(`name`) => true
            case _ => false
          })

        def removePrivacyAnnotations(mods: Modifiers): Modifiers =
          mods.mapAnnotations(_.filter({
            case SimpleAnnotation(PublicName | NoPrivacyName) => false
            case _ => true
          }))

        val flagsThatPreventPrivatization: FlagSet =
          PRIVATE | PROTECTED | OVERRIDE | ABSTRACT | SYNTHETIC |
            CASEACCESSOR | PARAMACCESSOR | PARAM | MACRO

        def shouldPrivatize(d: MemberDef): Boolean = {
          def isConsoleSpecialCase = currentHierarchy match {
            case name :: _ if name.matches("""res\d+|.*\$.*""") =>
              true
            case _ :: parentName :: _ if parentName.matches("""\$eval|\$iw""") =>
              true
            case _ =>
              false
          }

          d.mods.hasNoFlags(flagsThatPreventPrivatization) &&
            d.name != nme.CONSTRUCTOR &&
            !hasSimpleAnnotation(d.mods, PublicName) &&
            !isConsoleSpecialCase

        }

        def transformModifiers(d: MemberDef): Modifiers = {
          if (shouldPrivatize(d)) {
            printNoPrivacyHint
            reporter.info(d.pos, s"$phaseName made `${d.name}` $defaultVisibilityString.", force = true)
            // println(currentHierarchy.mkString(" <- "))

            d.mods.copy(flags = d.mods.flags | defaultVisibilityFlags)
          } else {
            removePrivacyAnnotations(d.mods)
          }
        }

        def transformOrSkip[T <: MemberDef](tree: T, cloner: (T, Modifiers) => T): T = {
          if (hasSimpleAnnotation(tree.mods, NoPrivacyName)) {
            reporter.info(tree.pos, s"$phaseName won't alter privacy in `${tree.name}`.", force = true)
            // Just remove the @noprivacy modifier and skip the whole subtree:
            cloner(tree, removePrivacyAnnotations(tree.mods))
          } else {
            // Recursively transform the tree and alter its modifiers.
            // Assume transform returns same type, which is true in our cases.
            cloner(super.transform(tree).asInstanceOf[T], transformModifiers(tree))
          }
        }

        private[this] var currentHierarchy = List[String]()
        def enter[T](name: Name)(b: => T): T = {
          currentHierarchy = name.toString :: currentHierarchy
          try {
            b
          } finally {
            currentHierarchy = currentHierarchy.tail
          }
        }

        override def transform(tree: Tree) = tree match {
          case d @ ValDef(mods, name, tpt, rhs) =>
            enter(name) {
              transformOrSkip(d, (t: ValDef, mods: Modifiers) => t.copy(mods = mods))
            }

          case d @ DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
            enter(name) {
              transformOrSkip(d, (t: DefDef, mods: Modifiers) => t.copy(mods = mods))
            }

          case d @ ClassDef(mods, name, tparams, impl) =>
            enter(name) {
              transformOrSkip(d, (t: ClassDef, mods: Modifiers) => t.copy(mods = mods))
            }

          case d @ ModuleDef(mods, name, impl) =>
            enter(name) {
              transformOrSkip(d, (t: ModuleDef, mods: Modifiers) => t.copy(mods = mods))
            }

          case _ =>
            super.transform(tree)
        }
      } transform unit.body
    }
  }
}
