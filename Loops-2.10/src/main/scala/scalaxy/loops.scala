package scalaxy

import scala.language.experimental.macros
import scala.language.implicitConversions

import scala.reflect.NameTransformer
import scala.reflect.macros.Context
import scala.reflect.NameTransformer.encode

/** Scala loops compilation optimizations.
 *  Currently limited to Range foreach loops (no support for yield / map yet).
 *  Requires "inline" ranges (so the macro can extract start, end and optional step),
 *  and step must be a constant.
 *
 *  General syntax:
 *  <code>for (i <- start [to/until] end [by step]) { ... }</code>
 *
 *  Examples:
 *  <pre><code>
 *    import scalaxy.loops._
 *    val n = 1000000
 *    for (i <- 0 until n optimized) { ... }
 *    for (i <- n to 10 by -3 optimized) { ... }
 *  </code></pre>
 */
package object loops
{
  // TODO: optimize Range.map.
  // TODO: optimize Array.foreach, Array.map.
  // TODO: optimize Array.tabulate.
  // TODO: optimize ArrayBuffer.foreach, ArrayBuffer.map.
  // TODO: optimize (List/Seq).apply(...).foreach (replace with Array.apply + while loop)
  implicit def rangeExtensions(range: Range) =
    new RangeExtensions(range)

  private[loops] class RangeExtensions(range: Range)
  {
    /** Ensures a Range's foreach loop is compiled as an optimized while loop.
     *  Failure to optimize the loop will result in a compilation error.
     */
    def optimized: OptimizedRange = ???
  }

  private[loops] class OptimizedRange
  {
    /** Optimized foreach method.
     *  Only works if `range` is an inline Range with a constant step.
     */
    def foreach[U](f: Int => U): Unit =
      macro impl.rangeForeachImpl[U]

    def withFilter(f: Int => Boolean): OptimizedRange = ???

    def filter(f: Int => Boolean): OptimizedRange = ???

    /** This must not be executed at runtime
     *  (should be rewritten away by the foreach macro during compilation).
     */
    ???
  }
}

package loops
{
  package object impl
  {
    lazy val disabled =
      System.getenv("SCALAXY_LOOPS_OPTIMIZED") == "0" ||
      System.getProperty("scalaxy.loops.optimized") == "false"

    // This needs to be public and statically accessible.
    def rangeForeachImpl[U]
        (c: Context)
        (f: c.Expr[Int => U]): c.Expr[Unit] =
    {
      import c.universe._
      import definitions._
      import Flag._

      case class InlineRange(
        start: Tree,
        end: Tree,
        stepOpt: Option[Tree],
        isInclusive: Boolean,
        filters: List[Function])

      object N {
        def unapply(name: Name): Option[String] = Option(name).map(_.toString)
      }

      object StartEndInclusive {
        def unapply(tree: Tree): Option[(Tree, Tree, Boolean)] =
          Option(tree) collect {
            case
              Apply(
                Select(
                  Apply(
                    Select(_, N("intWrapper")),
                    List(start)),
                  N(junctionName @ ("to" | "until"))),
                List(end)) =>
              (start, end, junctionName == "to")
          }
      }

      object StartEndStepInclusive {
        def unapply(tree: Tree): Option[(Tree, Tree, Tree, Boolean)] =
          Option(tree) collect {
            case
              Apply(
                Select(
                  StartEndInclusive(start, end, isInclusive),
                  N("by")),
                List(step)) =>
              (start, end, step, isInclusive)
          }
      }

      def ifInstanceOf[A: TypeTag](tree: Tree): Option[Tree] =
        if (tree != null && tree.tpe != null && tree.tpe <:< typeTag[A].tpe)
          Some(tree)
        else
          None

      object InlineRangeTree {
        def unapply(tree: Tree): Option[InlineRange] =
          Option(tree) collect {
            case StartEndInclusive(start, end, isInclusive) =>
              InlineRange(start, end, None, isInclusive, Nil)
            case StartEndStepInclusive(start, end, step, isInclusive) =>
              InlineRange(start, end, Some(step), isInclusive, Nil)
          }
      }

      object OptimizedRange {
        lazy val RangeType: Type = rootMirror.staticClass("scala.collection.immutable.Range").asType.toType
        // lazy val OptimizedRangeType: Type = rootMirror.staticModule("scalaxy.loops.OptimizedRange").asType.toType

        def unapply(tree: Tree): Option[(Tree, InlineRange)] =
          Option(tree) collect {
            case Select(Apply(_, List(rangeTree @ InlineRangeTree(range))), N("optimized"))
            if rangeTree.tpe <:< RangeType => //&& tree.tpe <:< OptimizedRangeType =>
              (rangeTree, range)
            case Apply(Select(OptimizedRange(rangeTree, range), N(n @ ("filter" | "withFilter"))), List(filter @ Function(_, _))) =>
              (
                Apply(Select(rangeTree, n: TermName), List(filter.duplicate)),
                range.copy(filters = range.filters :+ filter)
              )
          }
      }

      def newInlineAnnotation = {
        Apply(
          Select(
            New(Ident(typeOf[scala.inline].typeSymbol)),
            nme.CONSTRUCTOR),
          Nil)
      }

      c.typeCheck(c.prefix.tree) match {
        case OptimizedRange(rangeTree, range) =>
          if (disabled) {
            c.info(c.macroApplication.pos, "Loop optimizations are disabled.", true)
            c.Expr[Unit](Apply(Select(rangeTree, newTermName("foreach")), List(f.tree)))
          } else {
            // import range._
            val step: Int = range.stepOpt match {
              case Some(Literal(Constant(step: Int))) =>
                step
              case None =>
                1
              case Some(step) =>
                c.error(step.pos, "Range step must be a non-null constant!")
                0
            }
            c.typeCheck(f.tree) match {
              case Function(List(param), body) =>

                def newIntVal(name: TermName, rhs: Tree) =
                  ValDef(NoMods, name, TypeTree(IntTpe), rhs)

                def newIntVar(name: TermName, rhs: Tree) =
                  ValDef(Modifiers(MUTABLE), name, TypeTree(IntTpe), rhs)

                // Body expects a local constant: create a var outside the loop + a val inside it.
                val iVar = newIntVar(c.fresh("i"), range.start)
                val iVal = newIntVal(param.name, Ident(iVar.name))
                val filterDefs = range.filters.map {
                  case Function(vparams, body) =>
                    DefDef(
                      NoMods.mapAnnotations(list => newInlineAnnotation :: list),
                      c.fresh("filter"): TermName,
                      Nil,
                      List(vparams),
                      TypeTree(NoType),
                      body)
                    // ValDef(NoMods, c.fresh("filter"): TermName, TypeTree(typeOf[Int => Boolean]), filter)
                }
                val stepVal = newIntVal(c.fresh("step"), Literal(Constant(step)))
                val endVal = newIntVal(c.fresh("end"), range.end)
                val loopCondition =
                  Apply(
                    Select(
                      Ident(iVar.name),
                      newTermName(
                        encode(
                          if (step > 0) {
                            if (range.isInclusive) "<=" else "<"
                          } else {
                            if (range.isInclusive) ">=" else ">"
                          }
                        )
                      )
                    ),
                    List(Ident(endVal.name))
                  )

                val iValExpr = c.Expr[Unit](iVal)
                val loopConditionExpr = c.Expr[Boolean](loopCondition)
                // Body still refers to old function param symbol (which has same name as iVal).
                // We must wipe it out (alas, it's not local, so we must reset all symbols).
                // TODO: be less extreme, replacing only the param symbol (see branch replaceParamSymbols).
                val bodyExpr = c.Expr[Unit](c.resetAllAttrs(body))

                val incrExpr = c.Expr[Unit](
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
                val iVarRef = c.Expr[Int](Ident(iVar.name))
                val stepValRef = c.Expr[Int](Ident(stepVal.name))

                val loop =
                  if (filterDefs.isEmpty) {
                    reify {
                      while (loopConditionExpr.splice) {
                        iValExpr.splice
                        bodyExpr.splice
                        incrExpr.splice
                      }
                    }
                  } else {
                    val filterApplies: List[Tree] = filterDefs.map(filterDef => {
                      Apply(
                        Ident(filterDef.name),
                        // Select(Ident(filterDef.name), "apply": TermName), 
                        List(
                          Ident(iVar.name)
                        )
                      )
                    })
                    val filterConditionExpr = c.Expr[Boolean](
                      filterApplies.reduceLeft((a: Tree, b: Tree) => 
                        Apply(
                          Select(a, encode("&&"): TermName),
                          List(b)
                        )
                      )
                    )
                    reify {
                      while (loopConditionExpr.splice) {
                        if (filterConditionExpr.splice) {
                          iValExpr.splice
                          bodyExpr.splice
                        }
                        incrExpr.splice
                      }
                    }
                  }

                val res = c.Expr[Unit](
                  Block(
                    (iVar :: endVal :: stepVal :: filterDefs) :+ loop.tree: _*)
                )
                // println("res = " + res)
                res
              case _ =>
                c.error(f.tree.pos, s"Unsupported function: $f")
                null
            }
          }
        case _ =>
          c.error(c.prefix.tree.pos, s"Expression not recognized by the ranges macro: ${c.prefix.tree}")
          null
      }
    }
  }
}
