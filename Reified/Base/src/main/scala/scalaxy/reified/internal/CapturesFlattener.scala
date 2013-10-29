package scalaxy.reified
package internal

import scala.reflect.runtime.universe._

import scalaxy.reified.internal.CommonExtractors._
import scalaxy.reified.internal.CommonScalaNames._
import scalaxy.reified.internal.Utils._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import scalaxy.generic.trees._

private[reified] class CapturesFlattener(tree: Tree) extends Transformer {
  private val captureDefMods = Modifiers(Flag.PRIVATE | Flag.FINAL | Flag.LOCAL)

  private var nextId = 1
  private def nextName: TermName = {
    val name = internal.syntheticVariableNamePrefix + nextId
    nextId += 1
    name
  }

  private def newInlineAnnotation = {
    Apply(
      Select(
        New(Ident(typeOf[scala.inline].typeSymbol)),
        nme.CONSTRUCTOR),
      Nil)
  }

  private type FreeTermSymbolKey = Any
  private def key(sym: FreeTermSymbol): FreeTermSymbolKey = {
    sym
    // sym.name + "@" + sym.origin
  }

  private val captureDefs = ArrayBuffer[() => ValOrDefDef]()
  private val captureNames = mutable.HashMap[FreeTermSymbolKey, TermName]()

  private val capturesUsedAsVals = mutable.HashSet[FreeTermSymbol]()

  import Optimizer.{ getFreshNameGenerator, loopsTransformer }
  private val loops = loopsTransformer(getFreshNameGenerator(tree), transform _)

  def flatten = {
    val trans = transform(tree)
    val defs = captureDefs.map(_()).toList
    if (defs.isEmpty)
      trans
    else
      Block(defs, trans)
  }

  object FreeTermTree {
    def unapply(tree: Tree): Option[(FreeTermSymbol, Tree)] = {
      val sym = tree.symbol
      if (sym != null && sym.isFreeTerm)
        Some(sym.asFreeTerm -> tree)
      else
        tree match {
          case Apply(Apply(Select(_, N(n)), List(f)), _) if n.matches("ReifiedFunction\\d|hasReifiedValueToValue") =>
            unapply(f)
          case _ =>
            None
        }
    }
  }

  object Applyoid {
    def unapply(tree: Tree): Option[(Tree, List[Tree])] = Option(tree) collect {
      case Apply(Select(t, n), a) if n.toString == "apply" =>
        t -> a
      case Apply(t, a) =>
        t -> a
    }
  }

  override def transform(tree: Tree) = {
    //val sym = tree.symbol
    tree match {
      case Applyoid(FreeTermTree(sym, ft), params) =>
        Apply(
          replaceFreeTerm(sym, ft),
          params.map(transform _))
      case Applyoid(TypeApply(FreeTermTree(sym, ft), tparams), params) =>
        Apply(
          TypeApply(
            replaceFreeTerm(sym, ft),
            tparams.map(transform _)),
          params.map(transform _))
      case FreeTermTree(sym, ft) =>
        capturesUsedAsVals += sym
        replaceFreeTerm(sym, ft)
      case _ =>
        loops.applyOrElse(tree, (tree: Tree) => super.transform(tree))
      // case Applyoid(ReifiedValueTree(sym, valueTree, tpe), params) =>
      //   Apply(
      //     Ident(getFreeTermName(sym, valueTree, tpe)),
      //     params.map(transform _))
      // case Applyoid(TypeApply(ReifiedValueTree(sym, valueTree, tpe), tparams), params) =>
      //   Apply(
      //     TypeApply(
      //       Ident(getFreeTermName(sym, valueTree, tpe)),
      //       tparams.map(transform _)),
      //     params.map(transform _))
      // case ReifiedValueTree(sym, valueTree, tpe) =>
      //   capturesUsedAsVals += sym
      //   Ident(getFreeTermName(sym, valueTree, tpe))
      // case _ =>
      //   val sym = tree.symbol
      //   if (sym != null && sym.isFreeTerm && sym.asFreeTerm.isStable) {
      //     val tsym = sym.asFreeTerm
      //     capturesUsedAsVals += tsym
      //     println(s"tree = $tree, tsym = $tsym (isStable = ${tsym.isStable})")
      //     Ident(getFreeTermName(tsym, tree, sym.typeSignature))
      //   } else {
      //     loops.applyOrElse(tree, (tree: Tree) => super.transform(tree))
      //   }
    }
  }

  private def replaceFreeTerm(sym: FreeTermSymbol, tree: Tree) = {
    val staticTpe = sym.typeSignature
    if (!(staticTpe <:< typeOf[HasReified[_]])) {
      // println("STATIC TYPE: " + staticTpe)
      tree
    } else {
      Ident(captureNames.getOrElseUpdate(key(sym), {
        val n = nextName

        val reifiedValue = sym.value.asInstanceOf[HasReified[_]].reifiedValue
        val valueTree = reifiedValue.expr.tree
        val tpe = reifiedValue.valueTag.tpe

        val resolved = super.transform(valueTree)

        // for (dup <- captureNames.keys.find(k => k.toString == sym.toString)) {
        //   println("DUPLICATE: " + dup + " (" + dup.origin + " vs. " + sym.origin + ")")
        //   println("\teq: " + (dup == sym))
        //   println("\teq: " + (dup.name == sym.name))
        //   println("\teq: " + (dup.origin == sym.origin))
        // }
        captureDefs += (() => resolved match {
          case Function(vparams, body) if !capturesUsedAsVals(sym) =>
            // The problem here is that vparams don't have types yet (typer hasn't been run).
            // However we do have tpe at hand, so we extract arg types from it.
            val TypeRef(pre, sym, args) = tpe
            DefDef(
              captureDefMods.mapAnnotations(list => newInlineAnnotation :: list),
              n,
              Nil,
              List(
                vparams.zip(args) map {
                  case (vparam, arg) =>
                    ValDef(Modifiers(Flag.PARAM), vparam.name, TypeTree(arg), EmptyTree)
                }
              ),
              TypeTree(NoType),
              transform(body))

          case _ =>
            ValDef(captureDefMods, n, TypeTree(tpe), resolved)
        })
        n
      }))
    }
  }
}
