package scalaxy.loops

private[loops] trait UnusableSinks extends StreamSources {
  val global: scala.reflect.api.Universe
  import global._

  case object UnusableSink extends StreamSink
  {
    override def sinkOption = Some(this)

    override def outputNeeds = Set()

    override def emitSink(
        inputVars: TuploidValue[Tree],
        fresh: String => TermName,
        transform: Tree => Tree): StreamOpResult =
    {
      ???
    }
  }
}
