package scalaxy.reified

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._

import scalaxy.reified.internal.Optimizer
import scalaxy.reified.internal.CompilerUtils
import scalaxy.reified.internal.CommonExtractors._
import scalaxy.reified.internal.CommonScalaNames._
import scalaxy.reified.internal.Utils
import scalaxy.reified.internal.Utils._
import scala.tools.reflect.ToolBox

import scalaxy.generic.trees._

/**
 * Reified value wrapper.
 */
private[reified] trait HasReifiedValue[A] {
  private[reified] def reifiedValue: ReifiedValue[A]
  def valueTag: TypeTag[A]
  override def toString = s"${getClass.getSimpleName}(${reifiedValue.value}, ${reifiedValue.expr})"
}

/**
 * Reified value which can be created by {@link scalaxy.reified.reify}.
 * This object retains the runtime value passed to {@link scalaxy.reified.reify} as well as its
 * compile-time AST.
 */
final class ReifiedValue[A: TypeTag](
  /**
   * Original value passed to {@link scalaxy.reified.reify}
   */
  valueGetter: => A,
  /**
   * AST of the value.
   */
  exprGetter: => Expr[A])
    extends HasReifiedValue[A] {

  lazy val value = valueGetter
  lazy val expr = exprGetter

  override def reifiedValue = this
  override def valueTag = typeTag[A]

  /**
   * Compile the AST (using the provided lifter to convert captured values to ASTs).
   * @param lifter how to convert captured values
   * @param toolbox toolbox used to perform the compilation. By default, using a toolbox configured
   *     with all stable optimization flags available.
   * @param optimizeAST whether to apply Scalaxy AST optimizations or not
   *     (optimizations range from transforming function value objects into defs when possible,
   *     to transforming some foreach loops into equivalent while loops).
   */
  def compile(
    toolbox: ToolBox[universe.type] = internal.Utils.optimisingToolbox,
    optimizeAST: Boolean = true): () => A = {

    val ast: Tree = flatExpr.tree
    val finalAST = ast
    // if (optimizeAST) Optimizer.optimize(ast, toolbox)
    // else ast

    val result = CompilerUtils.compile(finalAST)

    () => result().asInstanceOf[A]
  }

  /**
   * Get the AST of this reified value, using the specified lifter for any
   * value that was captured by the expression.
   * @return a block which starts by declaring all the captured values, and ends with a value that
   * only contains references to these declarations.
   */
  def flatExpr: Expr[A] = {
    val replacer = new Transformer {
      override def transform(tree: Tree): Tree = {
        tree match {
          case Apply(TypeApply(Select(_, N("hasReifiedValueToValue"))), List(reifiedValueTree)) =>
            val sym = reifiedValueTree.symbol.asFreeTerm
            val reifiedValue = sym.value.asInstanceOf[HasReifiedValue[_]].reifiedValue
            println("reifiedValueTree = " + reifiedValueTree)
            println("reifiedValue = " + reifiedValue)
            val valueTree = transform(reifiedValue.expr.tree)

            val n: TermName = internal.syntheticVariableNamePrefix
            typeCheckTree(
              Block(
                List(ValDef(Modifiers(Flag.LOCAL), n, TypeTree(sym.typeSignature), valueTree)),
                Ident(n)
              ),
              sym.typeSignature
            )
          // case Apply(
          //   Select(
          //     HasReifiedValueWrapperTree(
          //       builderName,
          //       CaptureTag(_, _, captureIndex)),
          //     methodName),
          //   args) =>
          //   Apply(Select(Ident(capturedRefName(captureIndex): TermName), methodName), args)
          case _ =>
            super.transform(tree)
        }
      }
    }

    val result = newExpr[A](replacer.transform(simplifyGenericTree(expr.tree)))
    println(result)
    result
  }
}
