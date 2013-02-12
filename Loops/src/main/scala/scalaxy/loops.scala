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

                  def tc[T <: Tree](t: Tree, tpe: Type): T =
                    c.typeCheck(Block(t, Literal(Constant(()))), tpe) match {
                      case Block(List(tt), _) => tt.asInstanceOf[T]
                    }
                    
                  def newIntVal(name: TermName, rhs: Tree): ValDef =
                    tc(ValDef(NoMods, name, TypeTree(IntTpe), rhs), UnitTpe)

                  def newIntVar(name: TermName, rhs: Tree): ValDef =
                    tc(ValDef(Modifiers(MUTABLE), name, TypeTree(IntTpe), rhs), UnitTpe)

                  implicit def valRef(vd: ValDef) = new {
                    def apply() = c.typeCheck(Ident(vd.symbol), IntTpe)
                  }
                  // Body expects a local constant: create a var outside the loop + a val inside it.
                  val iVar = newIntVar(c.fresh("i"), start)
                  val iVal = newIntVal(param.name, iVar())
                  val stepVal = newIntVal(c.fresh("step"), Literal(Constant(step)))
                  val endVal = newIntVal(c.fresh("end"), end)
                  
                  println(s"i.tpe = ${iVal.tpe}")
                  /*
                  // Type-check a fake (ordered) block, to force creation of ValDef symbols:
                  val Block(
                    List(
                      iVar @ ValDef(_, _, _, _),
                      iVal @ ValDef(_, _, _, _),
                      stepVal @ ValDef(_, _, _, _),
                      endVal @ ValDef(_, _, _, _)
                    ),
                    _
                  ) = c.typeCheck {
                    val iVarRaw = newIntVar(c.fresh("i"), start)
                    Block(
                      iVarRaw,
                      newIntVal(param.name, Ident(iVarRaw.name)),
                      newIntVal(c.fresh("step"), Literal(Constant(step))),
                      newIntVal(c.fresh("end"), end),
                      Literal(Constant(()))
                    )
                  }*/
              
                  println("TYPECHECKED")
                  
                  // Replace any mention of the lambda parameter by a reference to iVal:
                  val replacedBody = new Transformer { override def transform(tree: Tree) = {
                    if (tree.symbol == param.symbol) 
                      iVal()
                    else
                      super.transform(tree)
                  }}.transform(body)
                  
                  val condition =
                    Apply(
                      Select(
                        iVar(),
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
                      List(endVal())
                    )

                  val iVarExpr = c.Expr[Unit](iVar)
                  val iValExpr = c.Expr[Unit](iVal)
                  val endValExpr = c.Expr[Unit](endVal)
                  val stepValExpr = c.Expr[Unit](stepVal)
                  val conditionExpr = c.Expr[Boolean](condition)
                  // Body still refers to old function param symbol (which has same name as iVal).
                  // We must wipe it out (alas, it's not local, so we must reset all symbols).
                  //val bodyExpr = c.Expr[Unit](replacedBody)//c.resetAllAttrs(body))

                  val incr = //c.Expr[Unit](
                    Assign(
                      iVar(),
                      Apply(
                        Select(
                          iVar(),
                          encode("+")
                        ),
                        List(stepVal())
                      )
                    )
                  //)
                  val loopBody = c.Expr[Unit](
                    Block(
                      iVal,
                      replacedBody,
                      incr,
                      Literal(Constant(()))
                    )
                  )
                      
                  val res = reify {
                    iVarExpr.splice
                    endValExpr.splice
                    stepValExpr.splice
                    while (conditionExpr.splice) {
                      loopBody.splice
                    }
                  }
                  println(s"res = $res")
                  c.Expr[Unit](c.typeCheck(res.tree))
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
