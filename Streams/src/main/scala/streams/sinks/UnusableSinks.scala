package scalaxy.streams

private[streams] trait UnusableSinks extends StreamComponents {
  val global: scala.reflect.api.Universe
  import global._

  case object UnusableSink extends StreamSink
  {
    override def describe = None

    override def lambdaCount = 0

    override def outputNeeds = Set()

    override def emit(input: StreamInput, outputNeeds: OutputNeeds, nextOps: OpsAndOutputNeeds): StreamOutput =
      ???
  }
}
