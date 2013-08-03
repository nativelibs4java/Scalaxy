package scalaxy.reified

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._

import scalaxy.reified.internal.CaptureTag
import scalaxy.reified.internal.Optimizer
import scalaxy.reified.internal.Utils
import scalaxy.reified.internal.Utils._
import scala.tools.reflect.ToolBox

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

    val ast = expr(lifter).tree
    val finalAST = {
      if (optimizeAST) {
        Optimizer.optimize(ast, toolbox)
      } else {
        ast
      }
    }

    val result = {
      try {
        toolbox.compile(toolbox.resetAllAttrs(finalAST))
      } catch {
        case ex1: Throwable =>
          try {
            toolbox.compile(finalAST)
          } catch {
            case ex2: Throwable =>
              ex1.printStackTrace()
              throw new RuntimeException("Compilation failed: " + ex1 + "\nSource:\n\t" + ast, ex1)
          }
      }
    }
    () => result().asInstanceOf[A]
  }

  /**
   * Flatten the reified values captured by this reified value's AST, and return an equivalent
   * reified value which does not contain any captured reified value.
   * All the other captures are shifted / retagged appropriately.
   */
  private def flattenCaptures(lifter: Lifter, offset: Int = 0): (Tree, Seq[(Tree, Type)]) = {
    val capturedTrees = collection.mutable.ArrayBuffer[(Tree, Type)]()
    val captureMap = collection.mutable.HashMap[Int, Int]()
    capturedTerms.zipWithIndex.foreach {
      case ((value: HasReifiedValue[_], valueType), i) =>
        val (subTree, subCaptures) = value.reifiedValue.flattenCaptures(
          lifter,
          offset + capturedTrees.size
        )
        capturedTrees ++= subCaptures
        captureMap(i) = offset + capturedTrees.size
        capturedTrees += (subTree -> NoType) // valueType is ReifiedSomething...
      case ((value, valueType), i) =>
        captureMap(i) = offset + capturedTrees.size
        try {
          capturedTrees += (lifter.lift(value, valueType, true).get -> valueType)
        } catch {
          case ex: Throwable =>
            throw new RuntimeException("Failed to lift capture: " + ex + "\n" + this, ex)
        }
    }
    (transformCaptureIndices(captureMap), capturedTrees.toList)
  }

  private[reified] def transformCaptureIndices(f: Int => Int): Tree = {
    (new Transformer {
      override def transform(tree: Tree): Tree = {
        tree match {
          case CaptureTag(tpe, ref, captureIndex) =>
            CaptureTag.construct(tpe, ref, f(captureIndex))
          case _ =>
            super.transform(tree)
        }
      }
    }).transform(taggedExpr.tree)
  }

  private def isReifiedValue(tpe: Type) = {
    tpe != null && tpe <:< typeOf[ReifiedValue[_]]
  }

  private def isHasReifiedValue(tpe: Type) = {
    tpe != null && tpe <:< typeOf[HasReifiedValue[_]]
  }

  private object HasReifiedValueWrapperTree {
    def unapply(tree: Tree): Option[(Name, Tree)] = {
      val tpe = tree.tpe
      if (isHasReifiedValue(tpe) && !isReifiedValue(tpe)) {
        Option(tree) collect {
          case Apply(Apply(TypeApply(builder, targs), List(value)), implicits) =>
            builder.symbol.name -> value
        }
      } else {
        None
      }

    }
  }

  /**
   * Get the AST of this reified value, using the specified lifter for any
   * value that was captured by the expression.
   * @return a block which starts by declaring all the captured values, and ends with a value that
   * only contains references to these declarations.
   */
  def expr(lifter: Lifter = Lifter.DEFAULT): Expr[A] = {
    val capturedValueTrees = new collection.mutable.HashMap[Int, Tree]()
    def capturedRefName(captureIndex: Int): TermName = internal.syntheticVariableNamePrefix + "capture$" + captureIndex

    val (flatTaggedExpr, flatCapturedTrees) = flattenCaptures(lifter)

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
            Apply(Select(Ident(capturedRefName(captureIndex)), methodName), args)
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
            Apply(Select(Ident(capturedRefName(captureIndex)), methodName), args)
          case CaptureTag(_, _, captureIndex) =>
            Ident(capturedRefName(captureIndex))
          case _ =>
            super.transform(tree)
        }
      }
    }

    val function = replacer.transform(flatTaggedExpr)
    newExpr[A](
      if (flatCapturedTrees.isEmpty)
        function
      else
        Block(
          (
            for (((capturedTree, tpe), captureIndex) <- flatCapturedTrees.zipWithIndex) yield {
              val transformedCapture = replacer.transform(capturedTree)
              ValDef(
                Modifiers(Flag.LOCAL | Flag.FINAL | Flag.PRIVATE),
                capturedRefName(captureIndex),
                TypeTree(if (tpe eq null) NoType else tpe),
                transformedCapture)
            }
          ).toList,
          function
        )
    )
  }
}

