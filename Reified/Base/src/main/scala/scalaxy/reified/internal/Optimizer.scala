package scalaxy.reified.internal

import scala.language.experimental.macros

import scala.reflect._
import scala.reflect.macros.Context
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._
import scala.tools.reflect.ToolBox

import scalaxy.reified.internal.Utils._

/**
 * Small AST optimizer that performs the following rewrites:
 * - transform function values into methods when they're only used as methods (frequent pattern with Scalaxy/Reified's function composition and capture of reified functions)
 * - TODO: add Range foreach loops optimizations from Scalaxy
 */
object Optimizer {

  def optimize(rawTree: Tree, toolbox: ToolBox[universe.type]): Tree = {
    val tree = typeCheckTree(toolbox.resetAllAttrs(rawTree))

    val functionSymbols = tree collect {
      case vd @ ValDef(mods, name, tpt, Function(vparams, body)) =>
        vd.symbol
    }

    assert(functionSymbols.forall(s => s != null && s != NoSymbol), "Some ValDefs have no symbol")

    val functionsUsedAsObjects = tree.collect {
      case Select(t, m) if functionSymbols.contains(t.symbol) && m.toString != "apply" =>
        t.symbol
      case ValDef(_, _, _, rhs) if functionSymbols.contains(rhs.symbol) =>
        rhs.symbol
    }

    val optimizableFunctions = functionSymbols.toSet -- functionsUsedAsObjects.toSet

    //println(s"functionSymbols = $functionSymbols")
    //println(s"functionsUsedAsObjects = $functionsUsedAsObjects")
    //println(s"optimizableFunctions = $optimizableFunctions")
    val functionsPromoter = new Transformer {
      override def transform(tree: Tree) = tree match {
        case ValDef(mods, name, tpt, Function(vparams, body)) if optimizableFunctions.contains(tree.symbol) =>
          //println(s"optimizing " + tree.symbol + " = " + tree)
          DefDef(mods, name, Nil, List(vparams), TypeTree(NoType), transform(body))
        case Apply(Select(t, m), args) if optimizableFunctions.contains(t.symbol) && m.toString == "apply" =>
          Apply(t, args.map(transform(_)))
        case _ =>
          super.transform(tree)
      }
    }
    val optimized = functionsPromoter.transform(tree)
    //println("Original tree:\n" + tree)
    //println("Optimized tree:\n" + optimized)

    optimized
  }
}
