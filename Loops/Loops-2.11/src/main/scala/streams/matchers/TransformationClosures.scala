package scalaxy.loops
import scala.collection.breakOut
import scala.collection.mutable.ListBuffer

private[loops] trait TransformationClosures extends TuploidValues with Strippers
{
  val global: scala.reflect.api.Universe
  import global._

  case class ClosureWiringResult(
    preStatements: List[Tree],
    postStatements: List[Tree],
    outputVars: TuploidValue)

  object TransformationClosure
  {
    def wireInputsAndOutputs(
      inputs: TuploidValue,
      inputSymbols: Set[Symbol],
      outputs: TuploidValue,
      inputVars: TuploidValue,
      outputPathToInputPath: Map[TuploidPath, TuploidPath],
      outputNeeds: Set[TuploidPath],
      fresh: String => TermName,
      transformer: Tree => Tree)
        : ClosureWiringResult =
    {

      val pre = ListBuffer[Tree]()
      val post = ListBuffer[Tree]()

      val outputVars = new TuploidTransformer {
        var underNeededParent = List[Boolean](false)
        override def transform(path: TuploidPath, t: TuploidValue) = {
          val needed = outputNeeds(path) || underNeededParent.head
          val reusableName: Option[TermName] =
            if (inputSymbols(t.alias)) {
              // t is already somewhere in inputs. find it.
              val inputPath = outputPathToInputPath(path)
              val inputVar = inputVars.get(inputPath)

              Some(inputVar.aliasName)
            } else {
              None
            }

          t match {
            case TupleValue(values, alias, aliasName) =>
              underNeededParent = needed :: underNeededParent

              val subValues = values.map({ case (i, value) =>
                (i, transform(path :+ i, value))
              })

              val newName = if (needed) {
                val tupleClass: TermName = "scala.Tuple" + subValues.size
                val subRefs = subValues.toList.sortBy(_._1).map(_._2.aliasName).map(Ident(_))
                val newName = fresh("tup")
                // pre += q"var $newName: $t = _"
                post += q"val $newName = $tupleClass(..$subRefs)"

                newName
              } else {
                EmptyName
              }

              underNeededParent = underNeededParent.tail
              TupleValue(subValues, aliasName = newName)

            case ScalarValue(value, alias, aliasName) =>
              if (needed && reusableName.isEmpty) {
                val n = fresh("_" + path.map(_ + 1).mkString("_"))

                // TODO prepare `val $n = _ ; { ..$statements; $n = $aliasName }`
                //pre += q"var $n: $t = _"
                if (value != EmptyTree) {
                  post += q"val $n = ${transformer(value)}"
                } else if (alias != NoSymbol) {
                  post += q"val $n = ${alias.name}"
                } else {
                  post += q"val $n = $aliasName"
                }
                ScalarValue(aliasName = n)
              } else {
                ScalarValue(
                  aliasName = reusableName.getOrElse(
                    if (alias != NoSymbol) alias.name.asInstanceOf[TermName] else EmptyName))
              }
          }
        }
      } transform (RootTuploidPath, outputs)

      ClosureWiringResult(pre.result, post.result, outputVars)
    }
  }
  case class TransformationClosure(
      inputs: TuploidValue,
      statements: List[Tree],
      outputs: TuploidValue)
  {
    import TransformationClosure._

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
        outputNeeds: Set[TuploidPath],
        fresh: String => TermName,
        transform: Tree => Tree): (List[Tree], TuploidValue) =
    {
      def replacements(symbols: TuploidValue, names: TuploidValue): List[(Symbol, Name)] = {
        val pairOption =
          if (symbols.alias != NoSymbol && names.aliasName.toString != "")
            Some(symbols.alias -> names.aliasName)
          else
            None

        (symbols, names) match {
          case (TupleValue(symbolValues, _, _), TupleValue(nameValues, _, _)) =>
            symbolValues.keySet.intersect(nameValues.keySet)
              .flatMap(i => replacements(symbolValues(i), nameValues(i))).toList ++ pairOption

          case _ =>
            pairOption.toList
        }
      }
      val repls = replacements(inputs, inputVars).toMap

      var replacer = new Transformer {
        override def transform(tree: Tree) = {
          repls.get(tree.symbol) match {
            case Some(by) =>
              Ident(by)

            case None =>
              super.transform(tree)
          }
        }
      }

      val fullTransform = (t: Tree) => transform(replacer.transform(t))

      val ClosureWiringResult(pre, post, outputVars) =
        wireInputsAndOutputs(
          inputs,
          inputSymbols,
          outputs,
          inputVars,
          outputPathToInputPath,
          outputNeeds,
          fresh,
          fullTransform)

      val results =
        pre ++
        (
          if (statements.isEmpty)
            Nil
          else
            Block(statements.map(fullTransform), EmptyTree) :: Nil
        ) ++
        post

      println(s"""
          Replaced: $statements
          With: $results
          Repls: $repls
      """)
      (results, outputVars)
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
