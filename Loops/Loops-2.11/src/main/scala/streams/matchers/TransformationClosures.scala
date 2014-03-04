package scalaxy.loops
import scala.collection.breakOut

private[loops] trait TransformationClosures extends TuploidValues with Strippers
{
  val global: scala.reflect.api.Universe
  import global._

  case class TransformationClosure(
      inputs: TuploidValue,
      statements: List[Tree],
      outputs: TuploidValue)
  {
    def hasTupleAliasReferencesInStatements = {
      val tupleAliases = inputs.collectTupleAliases
      statements.exists(_.exists(t => tupleAliases.contains(t.symbol)))
    }

    val inputSymbols = inputs.collectSymbols
    val inputTupleAliasSymbols = inputs.collectTupleAliases
    val outputSymbols = outputs.collectSymbols

    val usedInputs: Set[Symbol] = statements.flatMap(_.collect {
      case t: RefTree if inputSymbols(t.symbol) =>
        t.symbol
    })(breakOut)

    val usedTupleAliasInputs: Set[Symbol] = statements.flatMap(_.collect {
      case t: RefTree if inputTupleAliasSymbols(t.symbol) =>
        t.symbol
    })(breakOut)

    val producedOutputs: Set[Symbol] = statements.flatMap(_.collect {
      case t: DefTree if outputSymbols(t.symbol) =>
        t.symbol
    })(breakOut)

    val producedOutputPaths: Set[TuploidPath] = producedOutputs.map(s => outputs.find(s).get)

    val outputPathToInputPath: Map[TuploidPath, TuploidPath] =
      outputSymbols.filter(inputSymbols).map(s =>
        outputs.find(s).get -> inputs.find(s).get
      )(breakOut)

    def getPreviousReferencedTupleAliasPaths(nextReferencedTupleAliasPaths: Set[TuploidPath])
        : Set[TuploidPath] =
    {
      val closureReferencePaths =
        usedTupleAliasInputs.map(s => inputs.find(s).get)

      val transposedPaths =
        nextReferencedTupleAliasPaths.collect(outputPathToInputPath)

      closureReferencePaths ++ transposedPaths
    }

    def replaceClosureBody(
        inputVars: TuploidValue,
        fresh: String => TermName,
        transform: Tree => Tree): (List[Tree], TuploidValue) =
    {
      println("TODO replaceClosureBody !!!")
      (statements.map(transform), inputVars)
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
          (ScalarValue(alias = param.symbol), body)
      } collect {
        case (inputValue, BlockOrNot(statements, TuploidValue(outputValue))) =>
          TransformationClosure(inputValue, statements, outputValue)
      }
    }
  }
}
