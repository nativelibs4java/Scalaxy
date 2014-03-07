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
        inputVars: TuploidValue[Tree],
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

      // Early typing / symbolization.
      val Block(List(indexValDef, indexVarDef, pairDef, indexVarRef, indexValRef, pairRef), EmptyTree) = typed(q"""
        private[this] val $indexVar = 0;
        private[this] val $indexVal = $indexVar;
        private[this] val $pairName = ${inputVars.alias.get};
        $indexVar;
        $indexVal;
        $pairName;
        ()
      """)

      import compat._
      val TypeRef(pre, sym, List(_, _)) = typeOf[(Int, Int)]
      val tupleTpe = TypeRef(pre, sym, List(inputVars.tpe, typeOf[Int]))
      require(tupleTpe != null && tupleTpe != NoType)
      val outputVars =
        TupleValue[Tree](
          tupleTpe,
          Map(
            0 -> inputVars,
            1 -> ScalarValue(typeOf[Int], alias = Some(indexValRef))),
          alias = Some(pairRef))

      val StreamOpResult(streamPrelude, streamBody, streamEnding) =
        emitSub(outputVars, opsAndOutputNeeds, fresh, transform)

      StreamOpResult(
        // TODO pass source collection to canBuildFrom if it exists.
        prelude = List(q"""
          ..$streamPrelude
          $indexVarDef
        """),
        // TODO match params and any tuple extraction in body with streamVars, replace symbols with streamVars values
        body = List(q"""
          $indexValDef
          ..${if (needsPair) List(pairDef) else Nil}
          ..$streamBody
          $indexVarRef += 1
        """),
        ending = streamEnding
      )
    }
  }
}
