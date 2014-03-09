package scalaxy.loops
import scala.collection.breakOut
import scala.collection.mutable.ListBuffer

private[loops] trait TransformationClosures
  extends TuploidValues
  with Strippers
  with StreamResults
{
  val global: scala.reflect.api.Universe
  import global._

  case class ClosureWiringResult(
    preStatements: List[Tree],
    postStatements: List[Tree],
    outputVars: TuploidValue[Tree])

  object TransformationClosure
  {
    def isSideEffectFreeRef(tree: Tree) = tree match {
      case Ident(_) if {
        val s = tree.symbol
        s.isTerm && {
          val ts = s.asTerm
          ts.isVal && !ts.isLazy
        }
      } =>
        true

      case _ =>
        false
    }
    def wireInputsAndOutputs(
        inputs: TuploidValue[Symbol],
        inputSymbols: Set[Symbol],
        outputs: TuploidValue[Symbol],
        outputPathToInputPath: Map[TuploidPath, TuploidPath],
        streamInput: StreamInput,
        outputNeeds: OutputNeeds)
          : ClosureWiringResult =
    {
      import streamInput.{ fresh, transform }

      val pre = ListBuffer[Tree]()
      val post = ListBuffer[Tree]()

      val outputVars = new TuploidTransformer[Symbol, Tree] {
        var underNeededParent = List[Boolean](false)
        override def transform(path: TuploidPath, t: TuploidValue[Symbol]) = {
          val needed = outputNeeds(path) || underNeededParent.head
          val reusableName: Option[Tree] =
            if (t.alias.exists(inputSymbols)) {
              // t is already somewhere in inputs. find it.
              val inputPath = outputPathToInputPath(path)
              val inputVar = streamInput.vars.get(inputPath)

              inputVar.alias.map(_.duplicate)
            } else {
              None
            }

          def generateVarIfNeeded(tpe: Type, value: => Tree, sideEffectValues: => List[Tree]): Option[Tree] = {
            if (needed && reusableName.isEmpty) {
              val name = fresh("_" + path.map(_ + 1).mkString("_"))
              val uninitializedValue = Literal(Constant(defaultValue(tpe)))
              val Block(List(decl), ref) = typed(q"""
                private[this] var $name: $tpe = $uninitializedValue;
                $name
              """)
              pre += decl
              post += q"$ref = $value"

              Some(ref)
            } else {
              reusableName.orElse({
                post ++= sideEffectValues

                None
              })
            }
          }

          t match {
            case TupleValue(tpe, values, alias) =>
              underNeededParent = needed :: underNeededParent

              val subValues = values.map({ case (i, value) =>
                (i, transform(path :+ i, value))
              })

              val newAlias = generateVarIfNeeded(tpe, {
                val tupleClass = rootMirror.staticModule("scala.Tuple" + subValues.size)
                // val tupleRef = typed(q"$tupleClass")
                val subRefs = subValues.toList.sortBy(_._1).map(_._2.alias.get).map(_.duplicate)
                typed(q"$tupleClass(..$subRefs)")
              }, {
                // val subs = subValues.toList.sortBy(_._1).flatMap(_._2.alias)
                // t.collect {
                //   case tree if !isSideEffectFreeRef(tree) =>
                //     tree
                // }
                Nil
              })

              underNeededParent = underNeededParent.tail
              TupleValue[Tree](tpe, subValues, alias = newAlias)

            case ScalarValue(tpe, value, alias) =>
              val mappedValue = value.map(streamInput.transform)
              val newAlias =
                generateVarIfNeeded(
                  tpe,
                  mappedValue.getOrElse(typed(Ident(alias.get))),
                  mappedValue.filterNot(isSideEffectFreeRef(_)).toList)

              // println(s"""
              //   t = $t
              //   tpe = $tpe
              //   newAlias = $newAlias
              //   reusableName = $reusableName
              // """)
              ScalarValue(tpe, alias = newAlias)
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

    val usedInputs: Set[Symbol] = (statements ++ outputs.collectValues).flatMap(_.collect {
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
      nextReferencedPaths: Set[TuploidPath],
      isMapLike: Boolean = true)
        : Set[TuploidPath] =
    {
      val closureReferencePaths =
        usedInputs.map(s => inputs.find(s).get)

      val transposedPaths =
        if (isMapLike)
          nextReferencedPaths.collect(outputPathToInputPath)
        else
          nextReferencedPaths

      // println(s"""
      //   usedInputs = $usedInputs
      //   inputs = $inputSymbols
      //   outputPathToInputPath = $outputPathToInputPath
      //   transposedPaths = $transposedPaths
      // """)
      closureReferencePaths ++ transposedPaths
    }

    def replacements(symbols: TuploidValue[Symbol], names: TuploidValue[Tree]): List[(Symbol, Tree)] = {
      val pairOption: Option[(Symbol, Tree)] =
        (symbols.alias, names.alias) match {
          case (Some(symbol), Some(name)) =>
            Some(symbol -> name)
          case _ =>
            None
        }

      (symbols, names) match {
        case (TupleValue(_, symbolValues, _), TupleValue(_, nameValues, _)) =>
          symbolValues.keySet.intersect(nameValues.keySet)
            .flatMap(i => replacements(symbolValues(i), nameValues(i))).toList ++ pairOption

        case _ =>
          pairOption.toList
      }
    }

    def getReplacer(inputVars: TuploidValue[Tree]): Tree => Tree = {
      val repls = replacements(inputs, inputVars).toMap

      var replacer = new Transformer {
        override def transform(tree: Tree) = {
          repls.get(tree.symbol) match {
            case Some(by) =>
              by.duplicate

            case None =>
              super.transform(tree)
          }
        }
      }

      // println(s"""
      //   repls = $repls
      //   inputs = $inputs
      //   outputs = $outputs
      //   inputVars = $inputVars
      // """)
      replacer.transform(_)
    }
    def replaceClosureBody(streamInput: StreamInput, outputNeeds: OutputNeeds): (List[Tree], TuploidValue[Tree]) =
    {
      import streamInput.{ fresh, transform }

      val replacer = getReplacer(streamInput.vars)
      val fullTransform = (tree: Tree) => transform(replacer(tree))

      val ClosureWiringResult(pre, post, outputVars) =
        wireInputsAndOutputs(
          inputs,
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

  object SomeTransformationClosure {
    def unapply(closure: Tree): Option[TransformationClosure] = {

      Option(closure) collect {
        case q"""($param) => ${Strip(pref @ Ident(_))} match {
          case ${CaseTuploidValue(inputValue, body)}
        }""" if param.name == pref.name =>
          (inputValue, body)

        case q"($param) => $body" =>
          (ScalarValue(param.tpe, alias = param.symbol.asOption), body)
      } collect {
        case (inputValue, BlockOrNot(statements, TuploidValue(outputValue))) =>
          TransformationClosure(inputValue, statements, outputValue)
      }
    }
  }
}
