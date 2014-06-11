package scalaxy.streams

private[streams] trait StreamComponents extends StreamResults {
  val global: scala.reflect.api.Universe
  import global._

  type OpsAndOutputNeeds = List[(StreamOp, OutputNeeds)]

  trait StreamComponent
  {
    def describe: Option[String]

    def sinkOption: Option[StreamSink]

    def lambdaCount: Int

    def emit(input: StreamInput,
             outputNeeds: OutputNeeds,
             nextOps: OpsAndOutputNeeds): StreamOutput

    protected def emitSub(input: StreamInput, nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      nextOps match {
        case (firstOp, outputNeeds) :: otherOpsAndOutputNeeds =>
          firstOp.emit(input, outputNeeds, otherOpsAndOutputNeeds)

        case Nil =>
          sys.error("Cannot call base emit at the end of an ops stream.")
      }
    }
  }

  trait StreamSource extends StreamComponent

  trait StreamOp extends StreamComponent
  {
    def transmitOutputNeedsBackwards(paths: Set[TuploidPath]): Set[TuploidPath]
  }

  trait StreamSink extends StreamOp
  {
  	/** If true, this sink is skipped unless it's at the end of the stream, i.e. after all ops. */
  	def isFinalOnly: Boolean = false
    override def sinkOption = Some(this)

    def outputNeeds: Set[TuploidPath] = Set(RootTuploidPath)

    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) = {
      val needs = outputNeeds
      require(paths.isEmpty || paths == needs)

      needs
    }

    def requireSinkInput(input: StreamInput,
                         outputNeeds: OutputNeeds,
                         nextOps: OpsAndOutputNeeds) {
      require(nextOps.isEmpty,
        "Cannot chain ops through a sink (got nextOps = " + nextOps + ")")
      require(outputNeeds == this.outputNeeds,
        "Expected outputNeeds " + this.outputNeeds + " for sink, got " + outputNeeds)
    }
  }

  trait PassThroughStreamOp extends StreamOp {

    override def describe: Option[String] = None

    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) = paths

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
    	var (nextOp, nextOutputNeeds) :: subsequentOps = nextOps
    	nextOp.emit(input, nextOutputNeeds, subsequentOps)
    }
  }

  // Allow loose coupling between sources, ops and sinks traits:
  val SomeStreamSource: Extractor[Tree, StreamSource]
  val SomeStreamOp: Extractor[Tree, (Tree, List[StreamOp])]
  val SomeStreamSink: Extractor[Tree, (Tree, StreamSink)]

  private[streams] def printOps(ops: List[StreamOp]) {
  	println(s"ops = " + ops.map(_.getClass.getSimpleName).mkString("\n\t"))
    // println(s"ops = " + ops.mkString("\n\t"))
  }
}
