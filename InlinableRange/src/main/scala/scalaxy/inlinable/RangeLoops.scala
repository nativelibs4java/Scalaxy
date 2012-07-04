package scalaxy.inlinable

import language.experimental.macros
import reflect.makro.Context

import collection._

trait RangeLoops
extends TreeBuilders
with CommonMatchers
with InlinableRangeMatchers
{
  val universe: reflect.makro.Universe
  import universe._
  import definitions._

  def resetAllAttrs(tree: Tree): Tree
  
  def newWhileRangeLoop(
      fresh: String => String,
      start: Tree, 
      end: Tree, 
      step: Option[Tree], 
      isInclusive: Boolean, 
      param: ValDef, 
      body: Tree): Tree = 
  {
    val iVar = newVar(fresh("i"), IntClass.asType, start)
    val iVal = newVar(fresh("ii"), IntClass.asType, iVar())
    val stepVal = newVar(fresh("step"), IntClass.asType, step.getOrElse(newInt(1)))
    val endVal = newVal(fresh("end"), IntClass.asType, end)

    val transformedBody = resetAllAttrs(transform(body) {
      case tree if tree.symbol == param.symbol => iVal()
    })

    def positiveCondition =
      binOp(
        iVar(), 
        intOp(if (isInclusive) nme.LE else nme.LT), 
        endVal()
      )
      
    def negativeCondition =
      binOp(
        iVar(),
        intOp(if (isInclusive) nme.GE else nme.GT), 
        endVal()
      )
    
    val outerDecls = new mutable.ListBuffer[Tree]
    
    outerDecls += iVar
    outerDecls += endVal
    outerDecls += stepVal
    
    val condition = step match {
      case None | Some(PositiveIntConstant(_)) =>
        println("step(" + step + ") is > 0")
        positiveCondition
      case Some(NegativeIntConstant(_)) =>
        println("step(" + step + ") is < 0")
        negativeCondition
      case _ =>
        println("step(" + step + ") has unknown sign")
        // we don't know if the step is positive or negative: cool!
        val isPositiveVal = newVal(
          fresh("isStepPositive"), 
          BooleanClass.asType,
          binOp(stepVal(), intOp(nme.GT), newInt(0))
        )
        outerDecls += isPositiveVal
        
        boolOr(
          boolAnd(isPositiveVal(), positiveCondition),
          boolAnd(boolNot(isPositiveVal()), negativeCondition)
        )
    }
    
    println("condition = " + condition)

    Block(
      outerDecls.result ++
      List(
        newWhileLoop(
          fresh,
          condition,
          Block(
            iVal,
            transformedBody,
            Assign(iVar(), intAdd(iVar(), stepVal()))
          )
        )
      ),
      newUnit
    )
  }
}
