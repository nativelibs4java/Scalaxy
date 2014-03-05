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
    outputVars: TuploidValue[TermName])

  object TransformationClosure
  {
    def wireInputsAndOutputs(
      inputs: TuploidValue[Symbol],
      inputSymbols: Set[Symbol],
      outputs: TuploidValue[Symbol],
      inputVars: TuploidValue[TermName],
      outputPathToInputPath: Map[TuploidPath, TuploidPath],
      outputNeeds: Set[TuploidPath],
      fresh: String => TermName,
      transformer: Tree => Tree)
        : ClosureWiringResult =
    {

      val pre = ListBuffer[Tree]()
      val post = ListBuffer[Tree]()

      val outputVars = new TuploidTransformer[Symbol, TermName] {
        var underNeededParent = List[Boolean](false)
        override def transform(path: TuploidPath, t: TuploidValue[Symbol]) = {
          val needed = outputNeeds(path) || underNeededParent.head
          val reusableName: Option[TermName] =
            if (t.alias.exists(inputSymbols)) {
              // t is already somewhere in inputs. find it.
              val inputPath = outputPathToInputPath(path)
              val inputVar = inputVars.get(inputPath)

              inputVar.alias
            } else {
              None
            }

          t match {
            case TupleValue(values, alias) =>
              underNeededParent = needed :: underNeededParent

              val subValues = values.map({ case (i, value) =>
                (i, transform(path :+ i, value))
              })

              val newAlias = if (needed) {
                val tupleClass: TermName = "scala.Tuple" + subValues.size
                val subRefs = subValues.toList.sortBy(_._1).map(_._2.alias.get).map(Ident(_))
                val newName = fresh("tup")
                // pre += q"var $newName: $t = _"
                post += q"val $newName = $tupleClass(..$subRefs)"

                Some(newName)
              } else {
                None
              }

              underNeededParent = underNeededParent.tail
              TupleValue(subValues, alias = newAlias)

            case ScalarValue(value, alias) =>
              if (needed && reusableName.isEmpty) {
                val aliasName = fresh("_" + path.map(_ + 1).mkString("_"))

                // TODO prepare `val $n = _ ; { ..$statements; $n = $aliasName }`
                //pre += q"var $n: $t = _"
                (value, alias) match {
                  case (Some(value), _) =>
                    post += q"val $aliasName = ${transformer(value)}"

                  case (None, Some(alias)) =>
                    post += q"val $aliasName = ${alias.name}"
                }

                ScalarValue(alias = Some(aliasName))
              } else {
                ScalarValue(
                  alias = reusableName.orElse(alias.map(_.name.asInstanceOf[TermName])))
              }
          }
        }
      } transform (RootTuploidPath, outputs)

      ClosureWiringResult(pre.result, post.result, outputVars)
    }
  }
  case class TransformationClosure(
      inputs: TuploidValue[Symbol],
      statements: List[Tree],
      outputs: TuploidValue[Symbol])
  {
    import TransformationClosure._

    val inputSymbols: Set[Symbol] = inputs.collectAliases
    val outputSymbols: Set[Symbol] = outputs.collectAliases

    val usedInputs: Set[Symbol] = statements.flatMap(_.collect {
      case t: RefTree if inputSymbols(t.symbol) =>
        t.symbol
    })(breakOut)

    // val usedTupleAliasInputs: Set[Symbol] = statements.flatMap(_.collect {
    //   case t: RefTree if inputTupleAliasSymbols(t.symbol) =>
    //     t.symbol
    // })(breakOut)

    val producedOutputs: Set[Symbol] = statements.flatMap(_.collect {
      case t: DefTree if outputSymbols(t.symbol) =>
        t.symbol
    })(breakOut)

    val producedOutputPaths: Set[TuploidPath] = producedOutputs.map(s => outputs.find(s).get)

    val outputPathToInputPath: Map[TuploidPath, TuploidPath] =
      outputSymbols.filter(inputSymbols).map(s =>
        outputs.find(s).get -> inputs.find(s).get
      )(breakOut)

    def getPreviousReferencedPaths(
      nextReferencedTupleAliasPaths: Set[TuploidPath],
      isMapLike: Boolean = true)
        : Set[TuploidPath] =
    {
      val closureReferencePaths =
        usedInputs.map(s => inputs.find(s).get)

      val transposedPaths =
        if (isMapLike)
          nextReferencedTupleAliasPaths.collect(outputPathToInputPath)
        else
          nextReferencedTupleAliasPaths

      closureReferencePaths ++ transposedPaths
    }

    def replaceClosureBody(
        inputVars: TuploidValue[TermName],
        outputNeeds: Set[TuploidPath],
        fresh: String => TermName,
        transform: Tree => Tree): (List[Tree], TuploidValue[TermName]) =
    {
      def replacements(symbols: TuploidValue[Symbol], names: TuploidValue[TermName]): List[(Symbol, Name)] = {
        val pairOption: Option[(Symbol, Name)] =
          (symbols.alias, names.alias) match {
            case (Some(symbol), Some(name)) =>
              Some(symbol -> name)
            case _ =>
              None
          }

        (symbols, names) match {
          case (TupleValue(symbolValues, _), TupleValue(nameValues, _)) =>
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
          (ScalarValue(alias = param.symbol.asOption), body)
      } collect {
        case (inputValue, BlockOrNot(statements, TuploidValue(outputValue))) =>
          TransformationClosure(inputValue, statements, outputValue)
      }
    }
  }
}
