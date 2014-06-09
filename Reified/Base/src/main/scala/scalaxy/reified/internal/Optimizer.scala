package scalaxy.reified.internal

import scala.language.experimental.macros
import scala.language.implicitConversions

import scala.reflect._
import scala.reflect.macros.blackbox.Context
import scala.reflect.runtime.universe
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
private[scalaxy] object Optimizer {
  import universe._
  import definitions._

  def newInlineAnnotation = {
    Apply(
      Select(
        New(Ident(typeOf[scala.inline].typeSymbol)),
        nme.CONSTRUCTOR),
      Nil)
  }

  object RangeForeach {
    def unapply(tree: Tree) = Option(tree) collect {
      case Apply(
        TypeApply(
          Select(
            NumRange(rangeTpe, numTpe, start, end, Step(step), isInclusive, filters),
            foreachName()),
          List(u)),
        List(Function(List(param), body))) =>
        (rangeTpe, numTpe, start, end, step, isInclusive, filters, param, body)
      case Apply(
        Select(
          NumRange(rangeTpe, numTpe, start, end, Step(step), isInclusive, filters),
          foreachName()),
        List(Function(List(param), body))) =>
        (rangeTpe, numTpe, start, end, step, isInclusive, filters, param, body)
    }
  }

  def loopsTransformer(freshName: String => TermName, transform: Tree => Tree): PartialFunction[Tree, Tree] = {
    case RangeForeach(rangeTpe, IntTpe, start, end, step, isInclusive, filters, param, body) =>

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
      val bodyExpr = newExpr[Unit](transform(body))

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
  }
}
