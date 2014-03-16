// Author: Olivier Chafik (http://ochafik.com)
package scalaxy.loops

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
object LoopsComponent {
  val phaseName = "scalaxy-loops"
}
class LoopsComponent(
  val global: Global, runAfter: String = "typer")
    extends PluginComponent
    with StreamTransforms
    with TypingTransformers {
  import global._
  import definitions._
  import Flags._

  override val phaseName = LoopsComponent.phaseName

  override val runsRightAfter = Option(runAfter)
  override val runsAfter = runsRightAfter.toList
  override val runsBefore = List("patmat")

  // var nextTypedHolderId = 0
  // override def typed(tree: Tree, tpe: Type) = {
  //   try {
  //     // val out = new Transformer {
  //     //   override def transform(tree: Tree) = tree match {
  //     //     case ValDef(mods, name, tpt, rhs)
  //     //         if tree.symbol == null || tree.symbol == NoSymbol =>
  //     //       val rtpt = transform(tpt)
  //     //       val trhs = transform(rhs)
  //     //       val owner = NoSymbol
  //     //       val sym = owner.newTermSymbol(name)
  //     //       if (mods.hasFlag(Flag.MUTABLE))
  //     //         q"$mods var $sym: $rtpt = $trhs"
  //     //       else
  //     //         q"$mods val $sym: $rtpt = $trhs"

  //     //     case _ =>
  //     //       super.transform(tree)
  //     //   }
  //     // } transform tree
  //     typer.typed(tree, tpe)
  //     // tree

  //     // val n: TypeName = "typedHolder" + nextTypedHolderId
  //     // nextTypedHolderId += 1
  //     // val DefDef(_, _, _, _, _, Block(List(typed),  _)) = typer.typed(q"def $n { $tree }", tpe)
  //     // val ClassDef(_, _, _,
  //     //   Template(_, _, List(_,
  //     //     DefDef(_, _, _, _, _, Block(List(typed), _))))) = typer.typed(q"class $n { def f = { $tree } }", tpe)
  //     // typed
  //     // val Block(List(typed), _) = typer.typed(q"{ $tree; () }", tpe)
  //     // typed
  //     // tree
  //   } catch { case ex: Throwable =>
  //     throw new RuntimeException(tree.toString, ex)
  //   }
  // }

  override def newPhase(prev: Phase) = new StdPhase(prev) {
    def apply(unit: CompilationUnit) {
      val transformer = new TypingTransformer(unit) {

        override def transform(tree: Tree) = tree match {
          case SomeStream(stream) =>
            reporter.info(tree.pos, impl.optimizedStreamMessage(stream.describe()), force = true)
            // val result = typer.typed {
            // val result = resetAttrs {
            val result = {
            // val result = resetLocalAttrs({
              stream
                .emitStream(n => unit.fresh.newName(n): TermName, transform(_), localTyper.typed(_))
                .compose(localTyper.typed(_))
            }

            // val removedSymbols = collection.mutable.Set[Symbol]()
            // for (t <- result; if t.symbol != null) {
            //   t match {
            //     case DefDef(_, _, _, _, _, _)  =>
            //       removedSymbols += t.symbol
            //       t.symbol = NoSymbol
            //     case ValDef(_, _, _, _) =>
            //       removedSymbols += t.symbol
            //       t.symbol = NoSymbol
            //     case _ =>
            //   }
            // }
            // for (t <- result; if removedSymbols(t.symbol)) {
            //   t.symbol = NoSymbol
            // }

            // for (i @ Ident(n) <- result; if i.symbol != null && i.symbol != NoSymbol) {
            //   println(s"FOUND $i: ${i.symbol.owner.logicallyEnclosingMember}")
            // }
            // println(tree)
            // println(result)

            // typed(result)
            result

          case _ =>
            super.transform(tree)
        }
      }

      unit.body = transformer transform unit.body
    }
  }
}
