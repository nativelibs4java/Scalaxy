package scalaxy.inlinable

import language.experimental.macros
import reflect.makro.Context

trait RangeLoops
extends InlinableNames
with TreeBuilders
{
  val universe: reflect.makro.Universe
  import universe._
  import definitions._

  def newWhileRangeLoop(
      fresh: String => String,
      start: Int, 
      end: Int, 
      step: Int, 
      isInclusive: Boolean, 
      param: ValDef, 
      body: Tree): Tree = 
  {
    val iVar = newVar(fresh("i"), IntClass.asType, newInt(start))
    val iVal = newVar(fresh("ii"), IntClass.asType, iVar())
    val endVal = newVal(fresh("end"), IntClass.asType, newInt(end))

    val transformedBody = transform(body) {
      case tree if tree.symbol == param.symbol => iVal()
    }

    val conditionOp = IntClass.asType.member(
      if (isInclusive) {
        if (step < 0) nme.GT else nme.LT
      } else {
        if (step < 0) nme.GE else nme.LE
      }
    )
    val condition = binOp(iVar(), conditionOp, endVal())

    Block(
      List(
        iVar,
        endVal,
        newWhileLoop(
          fresh,
          condition,
          Block(
            iVal,
            transformedBody,
            Assign(iVar(), intAdd(iVar(), newInt(step)))
          )
        )
      ),
      newUnit
    )
    /*
    val transformedBody = transform(body) {
      case tree if param.symbol == tree.symbol => Ident("i")
    }

    val startExpr = c.Expr[Int](start.asInstanceOf[c.Tree])
    val endExpr = c.Expr[Int](end.asInstanceOf[c.Tree])
    val bodyExpr = c.Expr[Unit](transformedBody.asInstanceOf[c.Tree])

    c.reify {
      var ii = startExpr.splice
      val end = endExpr.splice
      while (ii < end) {
        val i = ii
        bodyExpr.splice
        ii += 1
      }
    }
    */
  }
}
