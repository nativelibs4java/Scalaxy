package scalaxy.reified

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._

import scalaxy.reified.internal.CaptureTag
import scalaxy.reified.internal.Optimizer
import scalaxy.reified.internal.CompilerUtils
import scalaxy.reified.internal.CommonExtractors._
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
  override def toString = s"${getClass.getSimpleName}(${reifiedValue.value}, ${reifiedValue.taggedExpr.tree}, ${reifiedValue.capturedTerms})"
}

/**
 * Reified value which can be created by {@link scalaxy.reified.reify}.
 * This object retains the runtime value passed to {@link scalaxy.reified.reify} as well as its
 * compile-time AST.
 * It also keeps track of the values captured by the AST in its scope, which are identified in the
 * AST by calls to {@link scalaxy.internal.CaptureTag} (which contain the index of the captured value
 * in the capturedTerms field of this reified value).
 */
final case class ReifiedValue[A: TypeTag](
  /**
   * Original value passed to {@link scalaxy.reified.reify}
   */
  val value: A,
  /**
   * AST of the value, with {@link scalaxy.internal.CaptureTag} calls wherever an external value
   * reference was captured.
   */
  val taggedExpr: Expr[A],
  /**
   * Runtime values of the references captured by the AST, along with their static type at the site
   * of the capture.
   * The order of captures matches {@link scalaxy.internal.CaptureTag#indexCapture}.
   */
  val capturedTerms: Seq[(AnyRef, Type)])
    extends HasReifiedValue[A] {

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
    lifter: Lifter = Lifter.DEFAULT,
    toolbox: ToolBox[universe.type] = internal.Utils.optimisingToolbox,
    optimizeAST: Boolean = true): () => A = {

    val (rawAst, topLevelCaptures) = expr(lifter)
    val ast: Tree = {
      if (topLevelCaptures.isEmpty)
        rawAst.tree
      else
        Function(
          for ((name, InjectedCapture(value, tpe)) <- topLevelCaptures.toList) yield {
            ValDef(
              Modifiers(Flag.LOCAL | Flag.PARAM | Flag.FINAL),
              name,
              TypeTree(if (tpe eq null) NoType else tpe),
              EmptyTree)
          },
          rawAst.tree
        )
    }
    val finalAST = {
      if (optimizeAST) {
        Optimizer.optimize(ast, toolbox)
      } else {
        ast
      }
    }

    val result = CompilerUtils.compile(finalAST)

    if (topLevelCaptures.isEmpty) {
      // No values to inject.
      () => result().asInstanceOf[A]
    } else {
      () =>
        {
          val instance = result()
          if (instance == null)
            null.asInstanceOf[A]
          else {
            val classLoader = Option(instance.getClass.getClassLoader)
              .getOrElse(Thread.currentThread.getContextClassLoader)
            val instanceMirror = runtimeMirror(classLoader).reflect(instance)
            val instanceType = instanceMirror.symbol.asType.toType
            val method = instanceType.member("apply": TermName)

            val args = topLevelCaptures.map(_._2.value)
            // println("METHOD: " + method + " (alternative: " + method.asTerm.alternatives.head)
            // println("ARGS: " + args.mkString(", "))
            instanceMirror.reflectMethod(method.asTerm.alternatives.head.asMethod)(args: _*).asInstanceOf[A]
          }
        }
    }
  }

  /**
   * Flatten the reified values captured by this reified value's AST, and return an equivalent
   * reified value which does not contain any captured reified value.
   * All the other captures are shifted / retagged appropriately.
   */
  private def flattenCaptures(lifter: Lifter, offset: Int = 0, forceConversion: Boolean = false): (Tree, Seq[Capture]) = {
    val captures = collection.mutable.ArrayBuffer[Capture]()
    val captureMap = collection.mutable.HashMap[Int, Int]()
    capturedTerms.zipWithIndex.foreach {
      case ((value: HasReifiedValue[_], valueType), i) =>
        val (subTree, subCaptures) = value.reifiedValue.flattenCaptures(
          lifter,
          offset + captures.size
        )
        captures ++= subCaptures
        captureMap(i) = offset + captures.size
        captures += LiftedCapture(subTree, NoType) // valueType is ReifiedSomething...
      case ((value, valueType), i) =>
        captureMap(i) = offset + captures.size
        captures += (lifter.lift(value, valueType, forceConversion) match {
          case Some(LiftResult(tree, inlinable)) =>
            if (inlinable) {
              InlinableLiftedCapture(tree, valueType)
            } else {
              LiftedCapture(tree, valueType)
            }
          case None =>
            if (forceConversion) {
              sys.error(s"Failed to lift this value: $value (static type: $valueType)")
            }
            InjectedCapture(value, valueType)
        })
    }
    // println("TAGGED EXPR: " + taggedExpr)
    // println("CAPTURES: " + captures)
    // println("CAPTURE MAP: " + captureMap + " (offset = " + offset + ")")

    (transformCaptureIndices(captureMap, captures.size), captures.toList)
  }

  private[reified] def transformCaptureIndices(f: Int => Int, captureCount: Int): Tree = {
    (new Transformer {
      override def transform(tree: Tree): Tree = {
        tree match {
          case CaptureTag(tpe, ref, captureIndex) =>
            // val newCaptureIndex = f(captureIndex)
            // assert(newCaptureIndex < captureCount, s"Invalid capture index: before transform = $captureIndex, after = $newCaptureIndex, count = $captureCount")
            // println("REPLACING CAPTURE TAG: " + tree)
            CaptureTag.construct(tpe, ref, f(captureIndex))
          case _ =>
            super.transform(tree)
        }
      }
    }).transform(taggedExpr.tree)
  }

  /**
   * Get the AST of this reified value, using the specified lifter for any
   * value that was captured by the expression.
   * @return a block which starts by declaring all the captured values, and ends with a value that
   * only contains references to these declarations.
   */
  def expr(lifter: Lifter = Lifter.DEFAULT): (Expr[A], Seq[(String, InjectedCapture)]) = {
    def capturedRefName(captureIndex: Int) = internal.syntheticVariableNamePrefix + "capture$" + captureIndex

    // println("TAGGED EXPR: " + taggedExpr)

    val (flatTaggedExpr, captures) = flattenCaptures(lifter)

    // println("FLAT EXPR: " + flatTaggedExpr)
    // println("CAPTURES:\n\t" + captures.mkString("\n\t"))

    val replacer = new Transformer {
      override def transform(tree: Tree): Tree = {
        tree match {
          case Apply(
            Select(
              HasReifiedValueWrapperTree(
                builderName,
                CaptureTag(_, _, captureIndex)),
              methodName),
            args) =>
            Apply(Select(Ident(capturedRefName(captureIndex): TermName), methodName), args)
          case Apply(
            Select(
              wrapped @ Apply(
                TypeApply(
                  Select(
                    _,
                    converterName),
                  List(tpt)),
                List(CaptureTag(tpe, _, captureIndex))),
              methodName),
            args) if converterName.toString == "hasReifiedValueToValue" =>
            //println(s"tpt = $tpt, $tpe = $tpe, (tpt.tpe =:= tpe) = ${tpt.tpe =:= tpe}")
            Apply(Select(Ident(capturedRefName(captureIndex): TermName), methodName), args)
          case CaptureTag(_, _, captureIndex) =>
            captures(captureIndex) match {
              case InlinableLiftedCapture(tree2, tpe) =>
                tree2.duplicate
              case _ =>
                Ident(capturedRefName(captureIndex): TermName)
            }
          case _ =>
            super.transform(tree)
        }
      }
    }

    val function = replacer.transform(simplifyGenericTree(flatTaggedExpr))
    val topLevelCaptures = captures.zipWithIndex.collect({
      case (capture @ InjectedCapture(_, _), captureIndex) =>
        capturedRefName(captureIndex) -> capture
    })
    val expr = newExpr[A](
      if (captures.isEmpty)
        function
      else
        Block(
          captures.zipWithIndex.collect({
            case (LiftedCapture(capturedTree, tpe), captureIndex) =>
              val transformedCapture = replacer.transform(capturedTree)
              ValDef(
                Modifiers(Flag.LOCAL | Flag.FINAL | Flag.PRIVATE),
                capturedRefName(captureIndex),
                TypeTree(if (tpe eq null) NoType else tpe),
                transformedCapture)
          }).toList,
          function
        )
    )
    (expr, topLevelCaptures)
  }
}

sealed trait Capture
case class LiftedCapture(tree: Tree, tpe: Type) extends Capture
case class InlinableLiftedCapture(tree: Tree, tpe: Type) extends Capture
case class InjectedCapture(value: Any, tpe: Type) extends Capture

