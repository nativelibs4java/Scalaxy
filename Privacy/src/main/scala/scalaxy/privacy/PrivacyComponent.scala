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
 *  > val Some(List(ast)) = intp.parse("@scalaxy.extension[Int] def str = self.toString")
 *  > nodeToString(ast)
 *  > val DefDef(mods, name, tparams, vparamss, tpt, rhs) = ast // play with extractors to explore the tree and its properties.
 */
class PrivacyComponent(
  val global: Global)
    extends PluginComponent {
  import global._
  import definitions._

  override val phaseName = "scalaxy-privacy"

  override val runsRightAfter = Option("parser")
  override val runsAfter = runsRightAfter.toList
  override val runsBefore = List[String]("namer")

  private val flagsThatPreventPrivatization = {
    import Flags._
    PRIVATE | PROTECTED |
      OVERRIDE | ABSTRACT | SYNTHETIC |
      CASEACCESSOR | PARAMACCESSOR | PARAM | MACRO
  }

  def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {
    def apply(unit: CompilationUnit) {
      unit.body = new Transformer {
        def isPublicAnnotation(ann: Tree): Boolean = ann match {
          case Apply(Select(New(Ident(tpt)), nme.CONSTRUCTOR), _) if tpt.toString == "public" =>
            true
          case _ =>
            false
        }

        def shouldPrivatize(mods: Modifiers, name: Name): Boolean = {
          val n = name.toString

          name != nme.CONSTRUCTOR &&
            mods.hasNoFlags(flagsThatPreventPrivatization) &&
            !n.contains("$") && !n.matches("res\\d+") && // Special cases for the console.
            !mods.annotations.exists(isPublicAnnotation _)
        }
        def alterPrivacy(mods: Modifiers, name: Name, pos: Position): Modifiers = {
          if (shouldPrivatize(mods, name)) {
            reporter.warning(pos, phaseName + " made " + name + " private.")
            mods.copy(flags = mods.flags | Flags.PRIVATE)
          } else {
            mods.mapAnnotations(anns => anns.filter(!isPublicAnnotation(_)))
          }
        }
        override def transform(tree: Tree) = tree match {

          case ValDef(mods, name, tpt, rhs) =>
            val res = ValDef(
              alterPrivacy(mods, name, tree.pos),
              name,
              transform(tpt),
              transform(rhs))
            res.symbol = tree.symbol
            res.tpe = tree.tpe
            res

          case DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
            val res = DefDef(
              alterPrivacy(mods, name, tree.pos),
              name,
              tparams.map(transform(_).asInstanceOf[TypeDef]),
              vparamss.map(_.map(transform(_).asInstanceOf[ValDef])),
              transform(tpt),
              transform(rhs)
            )
            res.symbol = tree.symbol
            res.tpe = tree.tpe
            res

          case _ =>
            super.transform(tree)
        }
      } transform unit.body
    }
  }
}
