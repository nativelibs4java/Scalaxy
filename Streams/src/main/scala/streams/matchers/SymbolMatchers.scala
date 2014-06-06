package scalaxy.streams
import scala.collection.breakOut
import scala.collection.mutable.ListBuffer

private[streams] trait SymbolMatchers
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
      inputSymbols: Set[Symbol],
      outputs: TuploidValue[Symbol],
      outputPathToInputPath: Map[TuploidPath, TuploidPath],
      streamInput: StreamInput,
      outputNeeds: OutputNeeds)
        : ClosureWiringResult =
  {
    import streamInput.{ fresh, transform, typed }

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
          case TupleValue(tpe, values, alias, _) =>
            underNeededParent = needed :: underNeededParent

            val subValues = values.map({ case (i, value) =>
              (i, transform(path :+ i, value))
            })

            val newAlias = generateVarIfNeeded(tpe, {
              val tupleClass = rootMirror.staticModule("scala.Tuple" + subValues.size)
              // val tupleRef = typed(q"$tupleClass")
              val subRefs = subValues.toList.sortBy(_._1).map(_._2.alias.get).map(_.duplicate)
              typed(q"$tupleClass(..$subRefs)")
            }, Nil)

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

  def getReplacer(inputs: TuploidValue[Symbol], inputVars: TuploidValue[Tree]): Tree => Tree = {
    val repls = getReplacements(inputs, inputVars).toMap

    var replacer = new Transformer {
      override def transform(tree: Tree) = {
        repls.get(tree.symbol) match {
          case Some(by) =>
            //println("Replaced " + tree + " by " + by)
            transform(by.duplicate)

          case None =>
            super.transform(tree)
        }
      }
    }

    replacer.transform(_)
  }

  def getReplacements(symbols: TuploidValue[Symbol], names: TuploidValue[Tree]): List[(Symbol, Tree)] = {
    val pairOption: Option[(Symbol, Tree)] =
      (symbols.alias, names.alias) match {
        case (Some(symbol), Some(name)) =>
          Some(symbol -> name)
        case _ =>
          None
      }

    (symbols, names) match {
      case (TupleValue(_, symbolValues, _, _), TupleValue(_, nameValues, _, _)) =>
        symbolValues.keySet.intersect(nameValues.keySet)
          .flatMap(i => getReplacements(symbolValues(i), nameValues(i))).toList ++ pairOption

      case _ =>
        pairOption.toList
    }
  }
}
