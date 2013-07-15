package scalaxy.reified

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._

import scalaxy.reified.CaptureConversions.Conversion
import scalaxy.reified.internal.CaptureTag
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
final case class ReifiedValue[A: TypeTag] private[reified] (
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
   * Compile the AST (using the provided conversion to convert captured values to ASTs).
   * Requires scala-compiler.jar to be in the classpath.
   * Note: with Sbt, you can put scala-compiler.jar in the classpath with the following setting:
   * <pre><code>
   *   libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _)
   * </code></pre>
   */
  def compile(
    conversion: CaptureConversions.Conversion = CaptureConversions.DEFAULT,
    toolbox: ToolBox[universe.type] = internal.Utils.optimisingToolbox): () => A = {

    val ast = expr(conversion).tree

    val result = {
      try {
        toolbox.compile(toolbox.resetAllAttrs(ast))
      } catch {
        case ex1: Throwable =>
          try {
            toolbox.compile(ast)
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
   * Get the AST of this reified value, using the specified conversion function for any
   * value that was captured by the expression.
   */
  def expr(conversion: CaptureConversions.Conversion = CaptureConversions.DEFAULT): Expr[A] = {
    //stableExpr(conversion)
    optimizedExpr(conversion)
  }

  /**
   * Naive AST resolution that inlines captured values in their reference site.
   * As this might instantiate captured collections more than needed, this should be dropped as
   * soon as optimizedExpr is stable.
   */
  private def stableExpr(conversion: CaptureConversions.Conversion = CaptureConversions.DEFAULT): Expr[A] = {
    val transformer = new Transformer {
      override def transform(tree: Tree): Tree = {
        tree match {
          case CaptureTag(_, _, captureIndex) =>
            val (capturedValue, valueType) = capturedTerms(captureIndex)
            val converter: CaptureConversions.Conversion = conversion.orElse({
              case _ =>
                sys.error(s"This type of captured value is not supported: $capturedValue")
            })
            converter((capturedValue, valueType, converter))
          case _ =>
            super.transform(tree)
        }
      }
    }
    newExpr[A](transformer.transform(taggedExpr.tree))
  }

  /**
   * Flatten the reified values captured by this reified value's AST, and return an equivalent
   * reified value which does not contain any captured reified value.
   * All the other captures are shifted / retagged appropriately.
   */
  private def flattenCaptures(conversion: CaptureConversions.Conversion, offset: Int = 0): (Tree, Seq[(Tree, Type)]) = {
    val capturedTrees = collection.mutable.ArrayBuffer[(Tree, Type)]()
    val captureMap = collection.mutable.HashMap[Int, Int]()
    capturedTerms.zipWithIndex.foreach {
      case ((value: HasReifiedValue[_], valueType), i) =>
        val (subTree, subCaptures) = value.reifiedValue.flattenCaptures(
          conversion,
          offset + capturedTrees.size
        )
        capturedTrees ++= subCaptures
        captureMap(i) = offset + capturedTrees.size
        capturedTrees += (subTree -> NoType) // valueType is ReifiedSomething...
      case ((value, valueType), i) =>
        captureMap(i) = offset + capturedTrees.size
        capturedTrees += (conversion((value, valueType, conversion)) -> valueType)
    }

    val captureIndexShifter = new Transformer {
      override def transform(tree: Tree): Tree = {
        tree match {
          case CaptureTag(tpe, ref, captureIndex) =>
            CaptureTag.construct(tpe, ref, captureMap(captureIndex))
          case _ =>
            super.transform(tree)
        }
      }
    }

    (captureIndexShifter.transform(taggedExpr.tree), capturedTrees.toList)
  }

  /**
   * Return a block which starts by declaring all the captured values, and ends with a value that
   * only contains references to these declarations.
   */
  private def optimizedExpr(conversion: CaptureConversions.Conversion = CaptureConversions.DEFAULT): Expr[A] = {
    val capturedValueTrees = new collection.mutable.HashMap[Int, Tree]()
    def capturedRefName(captureIndex: Int): TermName = "scalaxy$capture$" + captureIndex

    val (flatTaggedExpr, flatCapturedTrees) = flattenCaptures(conversion.orElse({
      case (value, tpe: Type, conversion: Conversion) =>
        sys.error(s"This type of captured value is not supported: $value")
    }))

    val replacer = new Transformer {
      override def transform(tree: Tree): Tree = {
        tree match {
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
                Modifiers(Flag.LOCAL),
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

