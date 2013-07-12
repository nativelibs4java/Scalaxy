package scalaxy.reified

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._

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
        case _: Throwable =>
          try {
            toolbox.compile(ast)
          } catch {
            case ex: Throwable =>
              throw new RuntimeException("Compilation failed: " + ex + "\nSource:\n\t" + ast, ex)
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
    // TODO debug optimized conversion and use it!
    stableExpr(conversion)
    //optimizedExpr(conversion)
  }

  /**
   * Flatten the reified values captured by this reified value's AST, and return an equivalent
   * reified value which does not contain any captured reified value.
   * All the other captures are shifted / retagged appropriately.
   */
  private[reified] def flatten(capturesOffset: Int = 0): ReifiedValue[A] = {
    val flatCapturedTerms = collection.mutable.ArrayBuffer[(AnyRef, Type)]()
    flatCapturedTerms ++= capturedTerms

    val transformer = new Transformer {
      override def transform(tree: Tree): Tree = {
        tree match {
          case CaptureTag(tpe, ref, captureIndex) =>
            capturedTerms(captureIndex) match {
              case (value: ReifiedValue[_], _) =>
                val sub = value.flatten(capturesOffset + capturedTerms.size)
                val subTree = sub.taggedExpr.tree

                flatCapturedTerms ++= sub.capturedTerms
                subTree /*
                if (value.taggedExpr.tree eq subTree)
                  subTree.duplicate
                else
                  subTree*/
              case _ =>
                CaptureTag.construct(tpe, ref, captureIndex + capturesOffset)
            }
          case _ =>
            super.transform(tree)
        }
      }
    }
    new ReifiedValue[A](
      value,
      newExpr[A](transformer.transform(taggedExpr.tree)),
      flatCapturedTerms.toList)
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
   * Return a block which starts by declaring all the captured values, and ends with a value that
   * only contains references to these declarations.
   */
  private def optimizedExpr(conversion: CaptureConversions.Conversion = CaptureConversions.DEFAULT): Expr[A] = {
    val flatCaptures = new collection.mutable.ArrayBuffer[(AnyRef, Type)]()
    flatCaptures ++= capturedTerms
    val captureValueTrees = new collection.mutable.HashMap[Int, Tree]()
    def captureRefName(captureIndex: Int): TermName = "scalaxy$capture$" + captureIndex

    // TODO predeclare the captures in a block that returns the function
    val function = (new Transformer {
      override def transform(tree: Tree): Tree = {
        tree match {
          case CaptureTag(_, _, captureIndex) =>
            val (capturedValue, valueType) = capturedTerms(captureIndex)
            capturedValue match {
              case hr: HasReifiedValue[_] =>
                val flatRef = hr.reifiedValue.flatten(flatCaptures.size)
                flatCaptures ++= flatRef.capturedTerms
                // TODO maybe no need to duplicate if tree was unchanged (when no capture)
                super.transform(hr.reifiedValue.taggedExpr.tree) //.duplicate)
              case _ =>
                val converter: CaptureConversions.Conversion = conversion.orElse({
                  case _ =>
                    sys.error(s"This type of captured value is not supported: $capturedValue")
                })
                captureValueTrees(captureIndex) = converter((capturedValue, valueType, converter))
                Ident(captureRefName(captureIndex))
            }
          case _ =>
            super.transform(tree)
        }
      }
    }).transform(taggedExpr.tree)

    val res = newExpr[A](
      if (flatCaptures.isEmpty)
        function
      else
        Block(
          (
            for {
              ((_, tpe), captureIndex) <- flatCaptures.zipWithIndex
              tree = captureValueTrees(captureIndex)
            } yield {
              ValDef(Modifiers(Flag.LOCAL), captureRefName(captureIndex), TypeTree(if (tpe == null) NoType else tpe), tree)
            }
          ).toList,
          function
        )
    )
    //println(s"res = $res")
    res //typeCheck(res)
  }
}

