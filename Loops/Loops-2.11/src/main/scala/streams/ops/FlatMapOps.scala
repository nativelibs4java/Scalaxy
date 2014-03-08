package scalaxy.loops

private[loops] trait FlatMapOps
    extends ClosureStreamOps
    with CanBuildFromSinks
{
  val global: scala.reflect.api.Universe
  import global._

  val SomeStream: Extractor[Tree, Stream]

  object SomeFlatMapOp {
    def unapply(tree: Tree): Option[(Tree, StreamOp)] = Option(tree) collect {
      case q"$target.flatMap[$tpt, $_](${Strip(Function(List(param), body))})($canBuildFrom)" =>
        (target, body match {
          case SomeStream(stream) =>
            NestedFlatMapOp(tpt.tpe, param, stream, canBuildFrom)

          case _ =>
            GenericFlatMapOp(tpt.tpe, param, body, canBuildFrom)
        })
    }
  }

  case class GenericFlatMapOp(tpe: Type, param: ValDef, body: Tree, canBuildFrom: Tree)
      extends ClosureStreamOp
  {
    override val sinkOption = Some(CanBuildFromSink(canBuildFrom))

    override def emitOp(
        inputVars: TuploidValue[Tree],
        outputNeeds: Set[TuploidPath],
        opsAndOutputNeeds: List[(StreamOp, Set[TuploidPath])],
        fresh: String => TermName,
        transform: Tree => Tree): StreamOpResult =
    {
      // TODO: type this.
      val itemVal = fresh("item")
      val (replacedStatements, outputVars) = transformationClosure.replaceClosureBody(
        inputVars = ScalarValue(tpe, alias = Some(Ident(itemVal.toString))),
        outputNeeds, fresh, transform)
      val StreamOpResult(streamPrelude, streamBody, streamEnding) =
        emitSub(outputVars, opsAndOutputNeeds, fresh, transform)

      StreamOpResult(
        prelude = streamPrelude,
        body = List(typed(q"""
          ..$replacedStatements;
          ..$streamPrelude;
          for ($itemVal <- ${outputVars.alias.get}) {
            ..$streamBody;
          }
          ..$streamEnding
        """)),
        ending = streamEnding
      )
    }
  }

  case class NestedFlatMapOp(tpe: Type, param: ValDef, subStream: Stream, canBuildFrom: Tree)
      extends StreamOp
  {
    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) = {
      subStream.ops.foldRight(paths)({ case (op, refs) =>
        op.transmitOutputNeedsBackwards(refs)
      })
    }

    override val sinkOption = Some(CanBuildFromSink(canBuildFrom))

    override def emitOp(
        inputVars: TuploidValue[Tree],
        outputNeeds: Set[TuploidPath],
        opsAndOutputNeeds: List[(StreamOp, Set[TuploidPath])],
        fresh: String => TermName,
        transform: Tree => Tree): StreamOpResult =
    {
      val subTransform = (tree: Tree) => {
        if (tree.symbol == param.symbol) {
          inputVars.alias.get.duplicate
        } else {
          transform(tree)
        }
      }
      val Stream(source, ops, sink) = subStream
      val SinkOp(outerSink) :: outerOpsRev = opsAndOutputNeeds.map(_._1).reverse
      val outerOps = outerOpsRev.reverse
      StreamOpResult(
        body = List(
          Stream(source, ops ++ outerOps, outerSink).emitStream(fresh, subTransform)))
    }
  }

}
