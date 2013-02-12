package scalaxy

import scala.language.experimental.macros

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
  implicit def rangeExtensions(range: Range) = new
  {
    /** Ensures a Range's foreach loop is compiled as an optimized while loop.
     *  Failure to optimize the loop will result in a compilation error.
     */
    def optimized = new
    {
      /** Optimized foreach method.
       *  Only works if `range` is an inline Range with a constant step.
       */
      def foreach[U](f: Int => U): Unit =
        macro impl.rangeForeachImpl[U]

      /** This must not be executed at runtime
       *  (should be rewritten away by the foreach macro during compilation).
       */
      ???
    }
  }
}

package loops
{
  package object impl
  {
    // This needs to be public and statically accessible.
    def rangeForeachImpl[U : c.WeakTypeTag]
        (c: Context)
        (f: c.Expr[Int => U]): c.Expr[Unit] =
    {
      import c.universe._
      import definitions._
      import Flag._

      object InlineRangeTree {
        def unapply(tree: Tree): Option[(Tree, Tree, Option[Tree], Boolean)] =
          Option(tree) collect {
            case StartEndInclusive(start, end, isInclusive) =>
              (start, end, None, isInclusive)
            case StartEndStepInclusive(start, end, step, isInclusive) =>
              (start, end, Some(step), isInclusive)
          }
      }

      object StartEndInclusive {
        def unapply(tree: Tree): Option[(Tree, Tree, Boolean)] =
          Option(tree) collect {
            case
              Apply(
                Select(
                  Apply(
                    Select(_, intWrapperName),
                    List(start)),
                  junctionName),
                List(end))
            if intWrapperName.toString == "intWrapper" &&
               (junctionName.toString == "to" || junctionName.toString == "until")
            =>
              (start, end, junctionName.toString == "to")
          }
      }
      object StartEndStepInclusive {
        def unapply(tree: Tree): Option[(Tree, Tree, Tree, Boolean)] =
          Option(tree) collect {
            case
              Apply(
                Select(
                  StartEndInclusive(start, end, isInclusive),
                  byName),
                List(step))
            if byName.toString == "by" =>
              (start, end, step, isInclusive)
          }
      }
      c.typeCheck(c.prefix.tree) match {
        case Select(Apply(_, List(range)), optimizedName)
        if optimizedName.toString == "optimized" =>
          range match {
            case InlineRangeTree(start, end, stepOpt, isInclusive) =>
              val step: Int = stepOpt match {
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
                  val iVar = newIntVar(c.fresh("i"), start)
                  val iVal = newIntVal(param.name, Ident(iVar.name))
                  val stepVal = newIntVal(c.fresh("step"), Literal(Constant(step)))
                  val endVal = newIntVal(c.fresh("end"), end)
                  val condition =
                    Apply(
                      Select(
                        Ident(iVar.name),
                        newTermName(
                          encode(
                            if (step > 0) {
                              if (isInclusive) "<=" else "<"
                            } else {
                              if (isInclusive) ">=" else ">"
                            }
                          )
                        )
                      ),
                      List(Ident(endVal.name))
                    )

                  val iVarExpr = c.Expr[Unit](iVar)
                  val iValExpr = c.Expr[Unit](iVal)
                  val endValExpr = c.Expr[Unit](endVal)
                  val stepValExpr = c.Expr[Unit](stepVal)
                  val conditionExpr = c.Expr[Boolean](condition)
                  // Body still refers to old function param symbol (which has same name as iVal).
                  // We must wipe it out (alas, it's not local, so we must reset all symbols).
                  val bodyExpr = c.Expr[Unit](c.resetAllAttrs(body))

                  val incrExpr = c.Expr[Unit](
                    Assign(
                      Ident(iVar.name),
                      Apply(
                        Select(
                          Ident(iVar.name),
                          encode("+")
                        ),
                        List(Ident(stepVal.name))
                      )
                    )
                  )
                  val iVarRef = c.Expr[Int](Ident(iVar.name))
                  val stepValRef = c.Expr[Int](Ident(stepVal.name))

                  reify {
                    iVarExpr.splice
                    endValExpr.splice
                    stepValExpr.splice
                    while (conditionExpr.splice) {
                      iValExpr.splice
                      bodyExpr.splice
                      incrExpr.splice
                    }
                  }
                case _ =>
                  c.error(f.tree.pos, s"Unsupported function: $f")
                  null
              }
            case _ =>
              c.error(range.pos, s"Unsupported range: $range")
              null
          }
        case _ =>
          c.error(c.prefix.tree.pos, s"Expression not recognized by the ranges macro: ${c.prefix.tree}")
          null
      }
    }
  }
}
