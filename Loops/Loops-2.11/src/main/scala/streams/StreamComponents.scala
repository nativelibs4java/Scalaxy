package scalaxy.loops

private[loops] trait StreamComponents extends StreamResults {
  val global: scala.reflect.api.Universe
  import global._

  trait StreamComponent
  {
    def describe: Option[String]

    def sinkOption: Option[StreamSink]

    def emitSub(inputVars: TuploidValue[Tree],
                opsAndOutputNeeds: List[(StreamOp, Set[TuploidPath])],
                fresh: String => TermName,
                transform: Tree => Tree): StreamOpOutput =
    {
      opsAndOutputNeeds match {
        case (firstOp, outputNeeds) :: otherOpsAndOutputNeeds =>
          firstOp.emitOp(inputVars, outputNeeds, otherOpsAndOutputNeeds, fresh, transform)

        case Nil =>
          sys.error("Cannot call emitSub at the end of an ops stream.")
      }
    }
  }

  // type StreamOp <: StreamComponent
  trait StreamOp extends StreamComponent
  {
    def transmitOutputNeedsBackwards(paths: Set[TuploidPath]): Set[TuploidPath]

    def emitOp(inputVars: TuploidValue[Tree],
               outputNeeds: Set[TuploidPath],
               opsAndOutputNeeds: List[(StreamOp, Set[TuploidPath])],
               fresh: String => TermName,
               transform: Tree => Tree): StreamOpOutput
  }

  trait StreamSource extends StreamComponent {
    def emitSource(outputNeeds: Set[TuploidPath],
                   opsAndOutputNeeds: List[(StreamOp, Set[TuploidPath])],
                   fresh: String => TermName,
                   transform: Tree => Tree): StreamOpOutput
  }

  trait StreamSink extends StreamOp {

    override def sinkOption = Some(this)

    // TODO override for ops that return Unit (then return Set())
    def outputNeeds: Set[TuploidPath] = Set(RootTuploidPath)

    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) = {
      val needs = outputNeeds
      require(paths.isEmpty || paths == needs)

      needs
    }

    def emitSink(inputVars: TuploidValue[Tree],
                 fresh: String => TermName,
                 transform: Tree => Tree): StreamOpOutput

    override def emitOp(
        inputVars: TuploidValue[Tree],
        outputNeeds: Set[TuploidPath],
        opsAndOutputNeeds: List[(StreamOp, Set[TuploidPath])],
        fresh: String => TermName,
        transform: Tree => Tree): StreamOpOutput =
    {
      require(opsAndOutputNeeds.isEmpty,
        "Cannot chain ops through a sink (got opsAndOutputNeeds = " + opsAndOutputNeeds + ")")
      require(outputNeeds == this.outputNeeds,
        "Expected outputNeeds " + this.outputNeeds + " for sink, got " + outputNeeds)

      emitSink(inputVars, fresh, transform)
    }
  }

  val SomeStreamSource: Extractor[Tree, StreamSource]
  val SomeStreamOp: Extractor[Tree, (Tree, List[StreamOp])]
  val SomeStreamSink: Extractor[Tree, (Tree, StreamSink)]
}
