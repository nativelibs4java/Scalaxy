package scalaxy.streams

private[streams] trait Streams
    extends StreamComponents
    with UnusableSinks
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
        Some(new Stream(tree, source, ops, sink, hasExplicitSink = true))

      case SomeStreamOp(SomeStreamSource(source), ops) =>
        findSink(source :: ops).filter(_ != InvalidSink)
          .map(sink => new Stream(tree, source, ops, sink, hasExplicitSink = false))

      case SomeStreamSource(source) =>
        findSink(List(source)).filter(_ != InvalidSink)
          .map(sink => new Stream(tree, source, Nil, sink, hasExplicitSink = false))

      case _ =>
        None
    }
  }

  case class Stream(
      tree: Tree,
      source: StreamSource,
      ops: List[StreamOp],
      sink: StreamSink,
      hasExplicitSink: Boolean)
  {
    // println("FOUND STREAM: " + describe())

    def isDummy: Boolean =
      ops.isEmpty && (!hasExplicitSink || sink.isJustAWrapper)

    def describe(describeSink: Boolean = true) =
      (source :: ops).flatMap(_.describe).mkString(".") +
      sink.describe.filter(_ => describeSink).map(" -> " + _).getOrElse("")

    val components: List[StreamComponent] =
      (source :: ops) :+ sink

    def lambdaCount: Int =
      components.map(_.lambdaCount).sum
    lazy val closureSideEffectss: List[List[SideEffect]] =
      components.flatMap(_.closureSideEffectss)
    lazy val subTrees: List[Tree] =
      components.flatMap(_.subTrees)

    private[streams] def computeOutputNeedsBackwards(sinkNeeds: Set[TuploidPath]) =
      ops.scanRight(sinkNeeds)({
        case (op, refs) =>
          op.transmitOutputNeedsBackwards(refs)
      })

    def emitStream(fresh: String => TermName,
                   transform: Tree => Tree,
                   typed: Tree => Tree,
                   currentOwner: Symbol,
                   untyped: Tree => Tree,
                   sinkNeeds: Set[TuploidPath] = sink.outputNeeds,
                   loopInterruptor: Option[Tree] = None): StreamOutput =
    {
      val sourceNeeds :: outputNeeds =
        computeOutputNeedsBackwards(sinkNeeds)

      val nextOps = ops.zip(outputNeeds) :+ (sink, sinkNeeds)
      // println(s"source = $source")
      // println(s"""ops =\n\t${ops.map(_.getClass.getName).mkString("\n\t")}""")
      // println(s"stream = ${describe()}")
      // println(s"outputNeeds = ${nextOps.map(_._2)}")
      source.emit(
        input = StreamInput(
          vars = UnitTreeScalarValue,
          loopInterruptor = loopInterruptor,
          fresh = fresh,
          transform = transform,
          currentOwner = currentOwner,
          typed = typed,
          untyped = untyped),
        outputNeeds = sourceNeeds,
        nextOps = nextOps)
    }
  }
}
