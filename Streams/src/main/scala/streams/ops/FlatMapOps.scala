package scalaxy.streams

private[streams] trait FlatMapOps
    extends ClosureStreamOps
    with CanBuildFromSinks
    with Streams
    with Strippers
{
  val global: scala.reflect.api.Universe
  import global._

  val SomeStream: Extractor[Tree, Stream]

  object SomeFlatMapOp {
    def unapply(tree: Tree): Option[(Tree, StreamOp)] = Option(tree) collect {
      case q"$target.flatMap[$tpt, $_](${fun @ Strip(Function(List(param), body))})($cbf)" =>
        (target, FlatMapOp(tpt.tpe, param, body, Some(cbf)))

      // Option.flatMap doesn't take a CanBuildFrom.
      case q"$target.flatMap[$tpt](${fun @ Strip(Function(List(param), body))})" =>
        (target, FlatMapOp(tpt.tpe, param, body, None))
    }
  }

  case class FlatMapOp(tpe: Type, param: ValDef, body: Tree, canBuildFrom: Option[Tree])
      extends ClosureStreamOp
  {
    val nestedStream: Option[Stream] = q"($param) => $body" match {
      case SomeTransformationClosure(TransformationClosure(_, Nil, ScalarValue(_, Some(SomeStream(stream)), _))) =>
        Some(stream)

      case _ =>
        None // TODO
    }

    override def describe = Some(
      "flatMap" + nestedStream.map("(" + _.describe(describeSink = false) + ")").getOrElse(""))

    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) = {
      super.transmitOutputNeedsBackwards(paths) ++
      nestedStream.toList.flatMap(_.ops.foldRight(paths)({ case (op, refs) =>
        op.transmitOutputNeedsBackwards(refs)
      }))
    }

    override val sinkOption = canBuildFrom.map(CanBuildFromSink(_))

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input.{ fresh, transform, typed }

      nestedStream match {
        case Some(stream) =>
          val replacer = getReplacer(transformationClosure.inputs, input.vars)
          val subTransformer = new Transformer {
            override def transform(tree: Tree) = {
              if (tree.symbol == param.symbol) {
                input.vars.alias.get.duplicate
              } else {
                super.transform(tree)
              }
            }
          }
          val subTransform = (tree: Tree) => subTransformer.transform(transform(tree))
          val (outerSink: StreamSink) :: outerOpsRev = nextOps.map(_._1).reverse
          val outerOps = outerOpsRev.reverse
          val modifiedStream = stream.copy(ops = stream.ops ++ outerOps, sink = outerSink)

          modifiedStream.emitStream(fresh, subTransform, typed).map(replacer)

        case None =>
          val itemVal = fresh("item")
          val (replacedStatements, outputVars) =
            transformationClosure.replaceClosureBody(
              input.copy(
                vars = ScalarValue(tpe, alias = Some(Ident(itemVal.toString))),
                outputSize = None,
                index = None),
              outputNeeds)

          val sub = emitSub(input.copy(vars = outputVars), nextOps)
          sub.copy(body = List(typed(q"""
            ..$replacedStatements;
            for ($itemVal <- ${outputVars.alias.get}) {
              ..${sub.body};
            }
          """)))
      }
    }
  }
}
