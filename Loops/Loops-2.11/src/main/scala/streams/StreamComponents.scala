package scalaxy.loops

private[loops] trait StreamComponents extends TransformationClosures {
  val global: scala.reflect.api.Universe
  import global._

  trait StreamComponent
  {
    def sinkOption: Option[StreamSink]

    def emitSub(inputVars: TuploidValue[Tree],
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

    def emitOp(inputVars: TuploidValue[Tree],
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

    def sinkOption = Some(this)

    def outputNeeds: Set[TuploidPath] = Set(RootTuploidPath)

    def emitSink(inputVars: TuploidValue[Tree],
                 fresh: String => TermName,
                 transform: Tree => Tree): StreamOpResult
  }

  case class SinkOp(sink: StreamSink) extends StreamOp
  {
    override val sinkOption = Some(sink)

    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) = ???

    override def emitOp(
        inputVars: TuploidValue[Tree],
        outputNeeds: Set[TuploidPath],
        opsAndOutputNeeds: List[(StreamOp, Set[TuploidPath])],
        fresh: String => TermName,
        transform: Tree => Tree): StreamOpResult =
    {
      require(opsAndOutputNeeds.isEmpty)

      sink.emitSink(inputVars, fresh, transform)
    }
  }
}
