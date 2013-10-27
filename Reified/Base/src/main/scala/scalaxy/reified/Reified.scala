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

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import scalaxy.generic.trees._

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

  private class CapturesFlattener(tree: Tree) extends Transformer {
    private var nextId = 1
    private def nextName: TermName = {
      val name = internal.syntheticVariableNamePrefix + nextId
      nextId += 1
      name
    }
    private val captureDefs = ArrayBuffer[() => ValOrDefDef]()
    private val captureNames = mutable.HashMap[FreeTermSymbol, TermName]()

    private val capturesUsedAsVals = mutable.HashSet[FreeTermSymbol]()

    import Optimizer.{ getFreshNameGenerator, loopsTransformer }
    val loops = loopsTransformer(getFreshNameGenerator(tree), transform _)

    def flatten = {
      val trans = transform(tree)
      val defs = captureDefs.map(_()).toList
      if (defs.isEmpty)
        trans
      else
        Block(defs, trans)
    }
    override def transform(tree: Tree) = {
      import ReifiedValueUtils._
      //val sym = tree.symbol
      tree match {
        //case Apply(Select(Ident(N("Predef")), N("intWrapper")), )
        case Applyoid(ReifiedValueTree(sym, valueTree, tpe), params) =>
          Apply(
            Ident(getFreeTermName(sym, valueTree, tpe)),
            params.map(transform _))
        case Applyoid(TypeApply(ReifiedValueTree(sym, valueTree, tpe), tparams), params) =>
          Apply(
            TypeApply(
              Ident(getFreeTermName(sym, valueTree, tpe)),
              tparams.map(transform _)),
            params.map(transform _))
        case ReifiedValueTree(sym, valueTree, tpe) =>
          capturesUsedAsVals += sym
          Ident(getFreeTermName(sym, valueTree, tpe))
        case _ =>
          val sym = tree.symbol
          if (sym != null && sym.isFreeTerm && sym.asFreeTerm.isStable) {
            val tsym = sym.asFreeTerm
            capturesUsedAsVals += tsym
            // println(s"tsym = $tsym (isStable = ${tsym.isStable})")
            Ident(getFreeTermName(tsym, tree, sym.typeSignature))
          } else {
            loops.applyOrElse(tree, (tree: Tree) => super.transform(tree))
          }
      }
    }

    private def getFreeTermName(sym: FreeTermSymbol, valueTree: Tree, tpe: Type) = {
      captureNames.getOrElseUpdate(sym, {
        val n = nextName
        val resolved = super.transform(valueTree)
        captureDefs += (() => resolved match {
          case Function(vparams, body) if !capturesUsedAsVals(sym) =>
            // The problem here is that vparams don't have types yet (typer hasn't been run).
            // However we do have tpe at hand, so we extract arg types from it.
            val TypeRef(pre, sym, args) = tpe
            DefDef(
              Modifiers(Flag.PRIVATE | Flag.FINAL).mapAnnotations(list => newInlineAnnotation :: list),
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
            ValDef(Modifiers(Flag.LOCAL), n, TypeTree(tpe), resolved)
        })
        //captureNames += sym -> n
        n
      })
    }
  }

  def flatExpr: Expr[A] = {
    import ReifiedValueUtils._
    val result = new CapturesFlattener(expr.tree).flatten

    println("RESULT: " + result)
    // for (
    //   t <- result
    // ) {
    //   //s <- Option(result.symbol)) {
    //   val tt = t.tpe;
    //   val s = result.symbol
    //   println(s"\t$t: $tt: $s")
    // }
    result.collect {
      case t if isHasReifiedValueFreeTerm(t.symbol) =>
        sys.error("RETAINED FREE TERM: " + t + " : " + t.symbol)
    }
    newExpr[A](result)
  }
  // def flatExpr0: Expr[A] = {
  //   import ReifiedValueUtils._
  //   val replacer = new Transformer {
  //     private def buildValDef(valueTree: Tree, tpe: Type) = {
  //       val n: TermName = internal.syntheticVariableNamePrefix
  //       valueTree match {
  //         case Function(vparams, body) =>
  //           // The problem here is that vparams don't have types yet (typer hasn't been run).
  //           // However we do have tpe at hand, so we extract arg types from it.
  //           val TypeRef(pre, sym, args) = tpe
  //           DefDef(
  //             Modifiers(Flag.PRIVATE | Flag.FINAL).mapAnnotations(list => newInlineAnnotation :: list),
  //             n,
  //             Nil,
  //             List(
  //               vparams.zip(args) map {
  //                 case (vparam, arg) =>
  //                   ValDef(Modifiers(Flag.PARAM), vparam.name, TypeTree(arg), EmptyTree)
  //               }
  //             ),
  //             TypeTree(NoType),
  //             transform(body))

  //         case _ =>
  //           ValDef(Modifiers(Flag.LOCAL), n, TypeTree(tpe), transform(valueTree))
  //       }
  //     }

  //     override def transform(tree: Tree): Tree = {
  //       import ReifiedValueUtils._
  //       tree match {
  //         case Applyoid(ReifiedValueTree(sym, valueTree, tpe), params) =>
  //           val vd = buildValDef(valueTree, tpe)
  //           Block(
  //             List(vd),
  //             Apply(Ident(vd.name), params))
  //         case Applyoid(TypeApply(ReifiedValueTree(sym, valueTree, tpe), tparams), params) =>
  //           val vd = buildValDef(valueTree, tpe)
  //           Block(
  //             List(vd),
  //             Apply(TypeApply(Ident(vd.name), tparams), params))
  //         case _ =>
  //           super.transform(tree)
  //       }
  //     }
  //   }
  //   val input = expr.tree
  //   var result = replacer.transform(input)
  //   // result = simplifyGenericTree(typeCheckTree(result, typeTag[A].tpe))
  //   // println("RESULT: " + result)

  //   newExpr[A](result)
  // }
}

private[reified] object ReifiedValueUtils {
  val PredefObject = currentMirror.staticModule("scala.Predef")

  private val hasReifiedValueTpe =
    currentMirror.staticClass("scalaxy.reified.HasReified").asType.toType

  private val reifiedPackageObject = currentMirror.staticModule("scalaxy.reified.package")

  def isHasReifiedValueFreeTerm(s: Symbol): Boolean =
    s != null && s.isFreeTerm && s.typeSignature <:< hasReifiedValueTpe

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
      val fsym = sym.asFreeTerm
      val reifiedValue = fsym.value.asInstanceOf[HasReified[_]].reifiedValue
      (fsym, reifiedValue.expr.tree, reifiedValue.valueTag.tpe)
    }
    private object ReifiedFunctionConstructor {
      def unapply(tree: Tree) = tree match {
        case Select(p, n) if /*reifiedPackageObject == p.symbol &&*/ n.toString.matches("ReifiedFunction\\d") =>
          // println("REIFIED PACKAGE IS: " + p + ": " + p.symbol)
          true
        case _ =>
          false
      }
    }
    def unapply(tree: Tree): Option[(FreeTermSymbol, Tree, Type)] = Option(tree) collect {
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
