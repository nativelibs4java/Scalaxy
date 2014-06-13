package scalaxy.streams

private[streams] trait Streams extends StreamComponents
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeStream extends Extractor[Tree, Stream] {
    def findSink(ops: List[StreamComponent]): Option[StreamSink] = {
      ops.reverse.toIterator.zipWithIndex.map({
        case (op, i) => (op.sinkOption, i)
      }) collectFirst {
        case (Some(sink), i) if !sink.isFinalOnly || i == 0 =>
          sink
      }
    }

    def unapply(tree: Tree): Option[Stream] = tree match {
      case SomeStreamSink(SomeStreamOp(SomeStreamSource(source), ops), sink) =>
        Some(new Stream(source, ops, sink))

      case SomeStreamOp(SomeStreamSource(source), ops) =>
        findSink(source :: ops).map(sink => new Stream(source, ops, sink))

      case _ =>
        None
    }
  }

  case class Stream(source: StreamSource, ops: List[StreamOp], sink: StreamSink)
  {
    def describe(describeSink: Boolean = true) =
      (source :: ops).flatMap(_.describe).mkString(".") +
      sink.describe.filter(_ => describeSink).map(" -> " + _).getOrElse("")

    def lambdaCount = ((source :: ops) :+ sink).map(_.lambdaCount).sum

    // TODO: refine this.
    def isWorthOptimizing(strategy: OptimizationStrategy) = {
      strategy match {
        case scalaxy.streams.optimization.none =>
          false

        case scalaxy.streams.optimization.safe =>
          // TODO: side-effects analysis to allow more cases where lambdaCount > 1
          lambdaCount == 1

        case scalaxy.streams.optimization.aggressive =>
          lambdaCount >= 1
      }
    }

    def emitStream(fresh: String => TermName,
                   transform: Tree => Tree,
                   typed: Tree => Tree,
                   sinkNeeds: Set[TuploidPath] = sink.outputNeeds): StreamOutput =
    {
      val sourceNeeds :: outputNeeds = ops.scanRight(sinkNeeds)({ case (op, refs) =>
        op.transmitOutputNeedsBackwards(refs)
      })
      val nextOps = ops.zip(outputNeeds) :+ (sink, sinkNeeds)
      // println(s"source = $source")
      // println(s"""ops =\n\t${ops.map(_.getClass.getName).mkString("\n\t")}""")
      // println(s"outputNeeds = ${nextOps.map(_._2)}")
      source.emit(
        input = StreamInput(
          vars = UnitTreeScalarValue,
          fresh = fresh,
          transform = transform,
          typed = typed),
        outputNeeds = sourceNeeds,
        nextOps = nextOps)
    }
  }
}
