package scalaxy.loops

private[loops] trait Streams extends TransformationClosures {
  val global: scala.reflect.api.Universe
  import global._

  trait StreamComponent
  {
    def sinkOption: Option[StreamSink]

    def emitSub(inputVars: TuploidValue,
                opsAndOutputNeeds: List[(StreamOp, Set[TuploidPath])],
                fresh: String => TermName,
                transform: Tree => Tree): StreamOpResult =
    {
      opsAndOutputNeeds match {
        case (firstOp, outputNeeds) :: otherOpsAndOutputNeeds =>
          firstOp.emitOp(inputVars, outputNeeds, otherOpsAndOutputNeeds, fresh, transform)

        case Nil =>
          sys.error("Cannot call emitSub at the end of an ops stream.")
      }
    }
  }

  case class StreamOpResult(prelude: List[Tree], body: List[Tree], ending: List[Tree])

  val NoStreamOpResult = StreamOpResult(prelude = Nil, body = Nil, ending = Nil)

  // type StreamOp <: StreamComponent
  trait StreamOp extends StreamComponent
  {
    def transmitOutputNeedsBackwards(paths: Set[TuploidPath]): Set[TuploidPath]

    def emitOp(inputVars: TuploidValue,
               outputNeeds: Set[TuploidPath],
               opsAndOutputNeeds: List[(StreamOp, Set[TuploidPath])],
               fresh: String => TermName,
               transform: Tree => Tree): StreamOpResult
  }

  trait StreamSource extends StreamComponent {
    def emitSource(outputNeeds: Set[TuploidPath],
                   opsAndOutputNeeds: List[(StreamOp, Set[TuploidPath])],
                   fresh: String => TermName,
                   transform: Tree => Tree): Tree
  }

  trait StreamSink extends StreamComponent {

    def outputNeeds: Set[TuploidPath]

    def emitSink(inputVars: TuploidValue,
                 fresh: String => TermName,
                 transform: Tree => Tree): StreamOpResult
  }

  case class SinkOp(sink: StreamSink) extends StreamOp
  {
    override val sinkOption = Some(sink)

    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) = ???

    override def emitOp(
        inputVars: TuploidValue,
        outputNeeds: Set[TuploidPath],
        opsAndOutputNeeds: List[(StreamOp, Set[TuploidPath])],
        fresh: String => TermName,
        transform: Tree => Tree): StreamOpResult =
    {
      require(opsAndOutputNeeds.isEmpty)

      sink.emitSink(inputVars, fresh, transform)
    }
  }

  case class Stream(source: StreamSource, ops: List[StreamOp], sink: StreamSink) {
    def emitStream(fresh: String => TermName,
                   transform: Tree => Tree): Tree =
    {
      val sinkNeeds = sink.outputNeeds
      val sourceNeeds :: outputNeeds = ops.scanRight(sinkNeeds)({ case (op, refs) =>
        op.transmitOutputNeedsBackwards(refs)
      })
      val opsAndOutputNeeds = ops.zip(outputNeeds) :+ ((SinkOp(sink), sinkNeeds))
      println(s"source = $source")
      println(s"""ops =\n\t${ops.map(_.getClass.getName).mkString("\n\t")}""")
      println(s"outputNeeds = ${opsAndOutputNeeds.map(_._2)}")
      source.emitSource(sourceNeeds, opsAndOutputNeeds, fresh, transform)
    }
  }

  trait StreamOpExtractor {
    def unapply(tree: Tree): Option[(StreamSource, List[StreamOp])]
  }

  val SomeStreamOp: StreamOpExtractor

  object SomeStream {
    def unapply(tree: Tree): Option[Stream] = tree match {
      case SomeStreamOp(source, ops @ (_ :: _)) =>
        (source :: ops).reverse.toIterator.map(_.sinkOption) collectFirst {
          case Some(sink) =>
            new Stream(source, ops, sink)
        }

      case _ =>
        None
    }
  }
}
