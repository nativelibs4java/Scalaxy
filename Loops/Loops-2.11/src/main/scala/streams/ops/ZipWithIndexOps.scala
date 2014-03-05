package scalaxy.loops

private[loops] trait ZipWithIndexOps
    extends StreamSources
    with TransformationClosures
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeZipWithIndexOp {
    def unapply(tree: Tree): Option[(Tree, ZipWithIndexOp.type)] = Option(tree) collect {
      case q"$target.zipWithIndex" =>
        (target, ZipWithIndexOp)
    }
  }

  case object ZipWithIndexOp extends StreamOp
  {
    override val sinkOption = None

    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) = {
      paths collect {
        // Only transmit _._1 and its children backwards
        case 0 :: sub =>
          sub
      }
    }

    override def emitOp(
        inputVars: TuploidValue[TermName],
        outputNeeds: Set[TuploidPath],
        opsAndOutputNeeds: List[(StreamOp, Set[TuploidPath])],
        fresh: String => TermName,
        transform: Tree => Tree): StreamOpResult =
    {
      // TODO wire input and output fiber vars 
      val indexVar = fresh("indexVar")
      val indexVal = fresh("indexVal")

      val needsPair: Boolean = outputNeeds(RootTuploidPath)
      val pairName: TermName = if (needsPair) fresh("zipWithIndexPair") else ""

      val tupleTpe = tq"scala.Tuple2[${inputVars.tpe}, Int]".tpe
      require(tupleTpe != null && tupleTpe != NoType)
      val outputVars =
        TupleValue[TermName](
          tupleTpe,
          Map(
            0 -> inputVars,
            1 -> ScalarValue(typeOf[Int], alias = Some(indexVal))),
          alias = Some(pairName))

      val StreamOpResult(streamPrelude, streamBody, streamEnding) =
        emitSub(outputVars, opsAndOutputNeeds, fresh, transform)

      def pairDef = q"val $pairName = ${inputVars.alias.get}"

      val builder = fresh("builder")
      StreamOpResult(
        // TODO pass source collection to canBuildFrom if it exists.
        prelude = List(q"""
          ..$streamPrelude
          val $indexVar = 0
        """),
        // TODO match params and any tuple extraction in body with streamVars, replace symbols with streamVars values
        body = List(q"""
          val $indexVal = $indexVar
          ..${if (needsPair) List(pairDef) else Nil}
          ..$streamBody
          $indexVar += 1
        """),
        ending = streamEnding
      )
    }
  }
}
