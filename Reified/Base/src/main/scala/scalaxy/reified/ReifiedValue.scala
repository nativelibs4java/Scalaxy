package scalaxy.reified

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror
import scala.reflect.runtime.universe._

import scalaxy.reified.internal.Optimizer
import scalaxy.reified.internal.CompilerUtils
import scalaxy.reified.internal.CommonExtractors._
import scalaxy.reified.internal.CommonScalaNames._
import scalaxy.reified.internal.Utils
import scalaxy.reified.internal.Utils._
import scala.tools.reflect.ToolBox

import scala.reflect.NameTransformer.encode

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

    // val result = CompilerUtils.compile(finalAST)
    val result = toolbox.compile(toolbox.resetLocalAttrs(finalAST))

    () => result().asInstanceOf[A]
  }

  private def newInlineAnnotation = {
    Apply(
      Select(
        New(Ident(typeOf[scala.inline].typeSymbol)),
        nme.CONSTRUCTOR),
      Nil)
  }

  def flatExpr: Expr[A] = {
    import ReifiedValueUtils._
    val replacer = new Transformer {
      private def buildValDef(valueTree: Tree, tpe: Type) = {
        val n: TermName = internal.syntheticVariableNamePrefix
        valueTree match {
          case Function(vparams, body) =>
            // The problem here is that vparams don't have types yet (typer hasn't been run).
            // However we do have tpe at hand, so we extract arg types from it.
            val TypeRef(pre, sym, args) = tpe
            DefDef(
              NoMods.mapAnnotations(list => newInlineAnnotation :: list),
              n,
              Nil,
              List(
                vparams.zip(args) map {
                  case (vparam, arg) =>
                    ValDef(Modifiers(Flag.PARAM), vparam.name, TypeTree(arg), EmptyTree)
                }
              ),
              TypeTree(NoType),
              transform(body))

          case _ =>
            ValDef(Modifiers(Flag.LOCAL), n, TypeTree(tpe), transform(valueTree))
        }
      }

      override def transform(tree: Tree): Tree = {
        import ReifiedValueUtils._
        val sym = tree.symbol
        tree match {
          case Applyoid(ReifiedValueTree(valueTree, tpe), params) =>
            val vd = buildValDef(valueTree, tpe)
            Block(
              List(vd),
              Apply(Ident(vd.name), params))
          case Applyoid(TypeApply(ReifiedValueTree(valueTree, tpe), tparams), params) =>
            val vd = buildValDef(valueTree, tpe)
            Block(
              List(vd),
              Apply(TypeApply(Ident(vd.name), tparams), params))
          case _ =>
            super.transform(tree)
        }
      }
    }
    val input = expr.tree
    var result = replacer.transform(input)
    // result = simplifyGenericTree(typeCheckTree(result, typeTag[A].tpe))
    // println("RESULT: " + result)

    newExpr[A](result)
  }
}

private[reified] object ReifiedValueUtils {
  val PredefObject = currentMirror.staticModule("scala.Predef")

  object Applyoid {
    def unapply(tree: Tree): Option[(Tree, List[Tree])] = Option(tree) collect {
      case Apply(Select(t, n), a) if n.toString == "apply" =>
        t -> a
      case Apply(t, a) =>
        t -> a
    }
  }
  object ReifiedValueTree {
    private def get(sym: Symbol) = {
      val reifiedValue = sym.asFreeTerm.value.asInstanceOf[HasReifiedValue[_]].reifiedValue
      reifiedValue.expr.tree -> reifiedValue.valueTag.tpe
    }
    private val hasReifiedValueTpe =
      currentMirror.staticClass("scalaxy.reified.HasReifiedValue").asType.toType

    private val reifiedPackageObject = currentMirror.staticModule("scalaxy.reified.package")

    private def isHasReifiedValueFreeTerm(s: Symbol): Boolean =
      s != null && s.isFreeTerm && s.typeSignature <:< hasReifiedValueTpe

    private object ReifiedFunctionConstructor {
      def unapply(tree: Tree) = tree match {
        case Select(p, n) if reifiedPackageObject == p.symbol && n.toString.matches("ReifiedFunction\\d") => true
        case _ => false
      }
    }
    def unapply(tree: Tree): Option[(Tree, Type)] = Option(tree) collect {
      case Apply(Apply(TypeApply(ReifiedFunctionConstructor(), _), List(f)), _) =>
        get(f.symbol)
      case Apply(Apply(ReifiedFunctionConstructor(), List(f)), _) =>
        get(f.symbol)
      case Select(v, N("value")) if isHasReifiedValueFreeTerm(v.symbol) =>
        get(v.symbol)
      case _ if isHasReifiedValueFreeTerm(tree.symbol) =>
        get(tree.symbol)
    }
  }
}
