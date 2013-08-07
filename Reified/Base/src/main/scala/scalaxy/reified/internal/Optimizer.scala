package scalaxy.reified.internal

import scala.language.experimental.macros
import scala.language.implicitConversions

import scala.reflect._
import scala.reflect.macros.Context
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._
import scala.reflect.NameTransformer.encode
import scala.tools.reflect.ToolBox

import scalaxy.reified.internal.Utils._
import scalaxy.reified.internal.CommonScalaNames._
import scalaxy.reified.internal.CommonExtractors._

/**
 * Small AST optimizer that performs the following rewrites:
 * - transform function values into methods when they're only used as methods (frequent pattern with Scalaxy/Reified's function composition and capture of reified functions)
 * - TODO: add Range foreach loops optimizations from Scalaxy
 */
object Optimizer {

  private def reset(tree: Tree, toolbox: ToolBox[universe.type]): Tree = {
    typeCheckTree(toolbox.resetAllAttrs(resolveModulePaths(universe)(tree)))
  }

  def optimize(rawTree: Tree, toolbox: ToolBox[universe.type] = Utils.optimisingToolbox): Tree = {
    val result = optimizeLoops(optimizeFunctionVals(rawTree, toolbox), toolbox)
    //val result = optimizeFunctionVals(rawTree, toolbox)
    //val result = reset(rawTree, toolbox)
    if (verbose) {
      println("Raw tree:\n" + rawTree)
      println("Optimized tree:\n" + result)
    }
    result
  }

  private def newInlineAnnotation = {
    Apply(
      Select(
        New(Ident(typeOf[scala.inline].typeSymbol)),
        nme.CONSTRUCTOR),
      Nil)
  }

  private def optimizeFunctionVals(rawTree: Tree, toolbox: ToolBox[universe.type]): Tree = {
    val tree = reset(rawTree, toolbox)

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

        case ValDef(mods, name, tpt, Function(vparams, body)) if optimizableFunctions(tree.symbol) =>
          //println(s"optimizing " + tree.symbol + " = " + tree)
          DefDef(
            mods.mapAnnotations(list => newInlineAnnotation :: list),
            name,
            Nil,
            List(vparams),
            TypeTree(NoType),
            transform(body))

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

  private def getFreshNameGenerator(tree: Tree): String => TermName = {
    val names = collection.mutable.HashSet[String]()
    names ++= tree.collect {
      case t if t.symbol != null && t.symbol.isTerm =>
        t.symbol.name.toString
    }

    (base: String) => {
      var i = 1;
      var name: String = null
      while ({ name = syntheticVariableNamePrefix + base + "$" + i; names.contains(name) }) {
        i += 1
      }
      names.add(name)
      name
    }
  }

  private def optimizeLoops(rawTree: Tree, toolbox: ToolBox[universe.type]): Tree = {
    import toolbox.resetAllAttrs

    val tree = reset(rawTree, toolbox)
    def freshName = getFreshNameGenerator(tree)

    val transformer = new Transformer {
      override def transform(tree: Tree) = tree match {
        case Apply(
          TypeApply(
            Select(
              IntRange(start, end, Step(step), isInclusive, filters),
              foreachName()),
            List(u)),
          List(Function(List(param), body))) =>

          def newIntVal(name: TermName, rhs: Tree) =
            ValDef(NoMods, name, TypeTree(typeOf[Int]), rhs)

          def newIntVar(name: TermName, rhs: Tree) =
            ValDef(Modifiers(Flag.MUTABLE), name, TypeTree(typeOf[Int]), rhs)

          // Body expects a local constant: create a var outside the loop + a val inside it.
          val iVar = newIntVar(freshName("i"), start)
          val iVal = newIntVal(param.name, Ident(iVar.name))
          val stepVal = newIntVal(freshName("step"), Literal(Constant(step)))
          val endVal = newIntVal(freshName("end"), end)
          val condition =
            Apply(
              Select(
                Ident(iVar.name),
                encode(
                  if (step > 0) {
                    if (isInclusive) "<=" else "<"
                  } else {
                    if (isInclusive) ">=" else ">"
                  }
                ): TermName
              ),
              List(Ident(endVal.name))
            )

          val iVarExpr = newExpr[Unit](iVar)
          val iValExpr = newExpr[Unit](iVal)
          val endValExpr = newExpr[Unit](endVal)
          val stepValExpr = newExpr[Unit](stepVal)
          val conditionExpr = newExpr[Boolean](condition)
          // Body still refers to old function param symbol (which has same name as iVal).
          // We must wipe it out (alas, it's not local, so we must reset all symbols).
          // TODO: be less extreme, replacing only the param symbol (see branch replaceParamSymbols).
          val bodyExpr = newExpr[Unit](resetAllAttrs(transform(body)))

          val incrExpr = newExpr[Unit](
            Assign(
              Ident(iVar.name),
              Apply(
                Select(
                  Ident(iVar.name),
                  encode("+"): TermName
                ),
                List(Ident(stepVal.name))
              )
            )
          )
          val iVarRef = newExpr[Int](Ident(iVar.name))
          val stepValRef = newExpr[Int](Ident(stepVal.name))

          universe.reify({
            iVarExpr.splice
            endValExpr.splice
            stepValExpr.splice
            while (conditionExpr.splice) {
              iValExpr.splice
              bodyExpr.splice
              incrExpr.splice
            }
          }).tree
        case _ =>
          super.transform(tree)
      }
    }
    transformer.transform(tree)
  }
}
