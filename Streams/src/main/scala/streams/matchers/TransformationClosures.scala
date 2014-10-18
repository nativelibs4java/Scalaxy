package scalaxy.streams
import scala.collection.breakOut
import scala.collection.mutable.ListBuffer

private[streams] trait TransformationClosures
  extends TuploidValues
  with Strippers
  with StreamResults
  with SymbolMatchers
{
  val global: scala.reflect.api.Universe
  import global._

  object Closure {
    def unapply(tree: Tree): Option[Function] = Option(tree) collect {
      case Strip(closure @ Function(List(_), _)) =>
        closure
    }
  }

  object SomeTransformationClosure {
    def unapply(closure: Tree): Option[TransformationClosure] = {

      Option(closure) collect {
        case q"""($param) => ${Strip(pref @ Ident(_))} match {
          case ${CaseTuploidValue(inputValue, body)}
        }""" if param.name == pref.name =>
          (inputValue, body)

        case q"($param) => $body" =>
          (ScalarValue(param.symbol.typeSignature, alias = param.symbol.asOption), body)
      } collect {
        case (inputValue, BlockOrNot(statements, TuploidValue(outputValue))) =>
          print(s"closureSymbol = ${closure.symbol}; tpe = ${closure.tpe}")
          TransformationClosure(inputValue, statements, outputValue, closureSymbol = closure.symbol)
      }
    }
  }

  case class TransformationClosure(
      inputs: TuploidValue[Symbol],
      statements: List[Tree],
      outputs: TuploidValue[Symbol],
      closureSymbol: Symbol)
  {
    private[this] val inputSymbols: Set[Symbol] = inputs.collectAliases
    private[this] val outputSymbols: Set[Symbol] = outputs.collectAliases

    private[this] def usedInputs: Set[Symbol] = (statements ++ outputs.collectValues).flatMap(_.collect {
      case t: RefTree if inputSymbols(t.symbol) =>
        t.symbol
    })(breakOut)

    private[this] val outputPathToInputPath: Map[TuploidPath, TuploidPath] =
      outputSymbols.filter(inputSymbols).map(s =>
        outputs.find(s).get -> inputs.find(s).get
      )(breakOut)

    def getPreviousReferencedPaths(
      nextReferencedPaths: Set[TuploidPath],
      isMapLike: Boolean = true)
        : Set[TuploidPath] =
    {
      val closureReferencePaths = usedInputs.map(inputs.find(_).get)

      val transposedPaths =
        if (isMapLike)
          nextReferencedPaths.collect(outputPathToInputPath)
        else
          nextReferencedPaths

      closureReferencePaths ++ transposedPaths
    }

    def replaceClosureBody(streamInput: StreamInput, outputNeeds: OutputNeeds): (List[Tree], TuploidValue[Tree]) =
    {
      import streamInput.{ fresh, transform, typed, currentOwner }

      val replacer = getReplacer(inputs, streamInput.vars)
      val fullTransform = (tree: Tree) => {
        transform(
          replaceDeletedOwner(
            replacer(tree),
            deletedOwner = closureSymbol,
            newOwner = currentOwner))
      }

      val ClosureWiringResult(pre, post, outputVars) =
        wireInputsAndOutputs(
          inputSymbols,
          outputs,
          outputPathToInputPath,
          streamInput.copy(transform = fullTransform),
          outputNeeds)

      val blockStatements = statements.map(fullTransform) ++ post
      val results =
        pre ++
        (
          if (blockStatements.isEmpty)
            Nil
          else
            List(Block(blockStatements.dropRight(1), blockStatements.last))
        )

      (results, outputVars)
    }
  }
}
