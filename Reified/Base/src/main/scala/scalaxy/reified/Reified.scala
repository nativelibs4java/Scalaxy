package scalaxy.reified

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror
import scala.reflect.runtime.universe._

import scalaxy.reified.internal.Optimizer
import scalaxy.reified.internal.Optimizer.newInlineAnnotation
import scalaxy.reified.internal.CapturesFlattener
import scalaxy.reified.internal.CompilerUtils
import scalaxy.reified.internal.CommonExtractors._
import scalaxy.reified.internal.CommonScalaNames._
import scalaxy.reified.internal.Utils
import scalaxy.reified.internal.Utils._
import scala.tools.reflect.ToolBox

import scala.reflect.NameTransformer.encode

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import scalaxy.generic.trees.simplifyGenericTree

/**
 * Reified value wrapper.
 */
private[reified] trait HasReified[A] {
  private[reified] def reifiedValue: Reified[A]
  def valueTag: TypeTag[A]
  override def toString = s"${getClass.getSimpleName}(${reifiedValue.value}, ${reifiedValue.expr})"
}

/**
 * Reified value which can be created by {@link scalaxy.reified.reify}.
 * This object retains the runtime value passed to {@link scalaxy.reified.reify} as well as its
 * compile-time AST.
 */
final class Reified[A: TypeTag](
  /**
   * Original value passed to {@link scalaxy.reified.reify}
   */
  valueGetter: => A,
  /**
   * AST of the value.
   */
  exprGetter: => Expr[A])
    extends HasReified[A] {

  lazy val value = valueGetter
  lazy val expr = exprGetter
  // {
  //   val x = exprGetter
  //   println("EXPR[" + valueTag.tpe + "]: " + x)
  //   x
  // }

  override def reifiedValue = this
  override def valueTag = typeTag[A]

  /**
   * Compile the AST.
   * @param toolbox toolbox used to perform the compilation. By default, using a
   *     toolbox configured with all stable optimization flags available.
   */
  def compile(toolbox: ToolBox[universe.type] = internal.Utils.optimisingToolbox): () => A = {

    var ast: Tree = flatExpr.tree
    // println("AST: " + ast)
    ast = simplifyGenericTree(toolbox.typeCheck(ast, valueTag.tpe))
    // println("SIMPLIFIED AST: " + ast)
    ast = toolbox.resetLocalAttrs(ast)
    // println("RESET AST: " + ast)
    def reinline(tree: Tree) = tree match {
      case DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
        DefDef(
          mods.mapAnnotations(list => newInlineAnnotation :: list),
          name, tparams, vparamss, tpt, rhs)
      case _ =>
        tree
    }
    ast = ast match {
      case Block(stats, expr) =>
        Block(stats.map(reinline _), expr)
      case _ =>
        ast
    }
    // println("REINLINED AST: " + ast)
    val result = toolbox.compile(ast)

    () => result().asInstanceOf[A]
  }

  def flatExpr: Expr[A] = {
    val result = new CapturesFlattener(expr.tree).flatten
    // result collect {
    //   case t if isHasReifiedValueFreeTerm(t.symbol) =>
    //     sys.error("RETAINED FREE TERM: " + t + " : " + t.symbol)
    // }
    newExpr[A](result)
  }
}
