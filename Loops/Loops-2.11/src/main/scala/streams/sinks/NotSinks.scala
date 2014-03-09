package scalaxy.loops

private[loops] trait UnusableSinks extends StreamComponents {
  val global: scala.reflect.api.Universe
  import global._

  case object UnusableSink extends StreamSink
  {
    override def describe = None

    override def sinkOption = Some(this)

    override def outputNeeds = Set()

    override def emitSink(
        inputVars: TuploidValue[Tree],
        fresh: String => TermName,
        transform: Tree => Tree): StreamOpOutput =
    {
      ???
    }
  }
}
