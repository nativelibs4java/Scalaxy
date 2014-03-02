package scalaxy.loops
import scala.collection.breakOut

private[loops] trait TransformationClosures extends TuploidValues
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
    })(scala.collection.breakOut)

    val usedTupleAliasInputs: Set[Symbol] = statements.flatMap(_.collect {
      case t: RefTree if inputTupleAliasSymbols(t.symbol) =>
        t.symbol
    })(scala.collection.breakOut)

    val producedOutputs: Set[Symbol] = statements.flatMap(_.collect {
      case t: DefTree if outputSymbols(t.symbol) =>
        t.symbol
    })(scala.collection.breakOut)

    val producedOutputPaths: Set[TuploidPath] = producedOutputs.map(s => outputs.find(s).get)

    val outputPathToInputPath: Map[TuploidPath, TuploidPath] =
      (for (s <- outputSymbols; if inputSymbols(s)) yield {
        // val inputPath = inputs.find(s)
        // val outputPath = outputs.find(s)
        // if (inputPath.isEmpty || outputPath.isEmpty)
        //   println(s"""
        //     INPUT PATH($s) = $inputPath, OUTPUT = $outputPath
        //     inputs = $inputs
        //     outputs = $outputs
        //   """)
        outputs.find(s).get -> inputs.find(s).get
      })(scala.collection.breakOut)
  }

  def getPreviousReferencedTupleAliasPaths(
    closure: TransformationClosure,
    nextReferencedTupleAliasPaths: Set[TuploidPath])
      : Set[TuploidPath] =
  {
    val closureReferencePaths =
      closure.usedTupleAliasInputs.map(s => closure.inputs.find(s).get)

    val transposedPaths =
      nextReferencedTupleAliasPaths.collect(closure.outputPathToInputPath)

    closureReferencePaths ++ transposedPaths
  }

  object TransformationClosure {
    def extract(closure: Tree): Option[TransformationClosure] = {

      val inputValueAndBodyOpt = closure match {
        case q"""($param) => (${paramRef @ Ident(_)}: $_) match { case $singleCase }"""
            if param.name == paramRef.name =>
          singleCase match {
            case CaseTuploidValue(inputValue, body) =>
              Some((inputValue, body))

            case _ =>
              None
          }

        case q"""($param) => $body""" =>
          Some((ScalarValue(param.symbol), body))

        case _ =>
          println("COULDN'T RECOGNIZE closure: " + closure)
          None
      }
      inputValueAndBodyOpt match {
        case Some((inputValue, Block(statements, returnValue))) =>
          returnValue match {
            case TuploidValue(outputValue) =>
              val result = TransformationClosure(inputValue, statements, outputValue)
              Option(result).filterNot(_.hasTupleAliasReferencesInStatements)

            case _ =>
              println("COULDN'T RECOGNIZE return value of body: " + returnValue)
              None
          }

        case Some((inputValue, body)) =>
          println("COULDN'T RECOGNIZE TuploidValue case: (inputValue = " + inputValue + "): " + body)
          None

        case _ =>
          None
      }
    }
  }
}
