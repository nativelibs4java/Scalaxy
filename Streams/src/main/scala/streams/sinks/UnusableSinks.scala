package scalaxy.streams

private[streams] trait UnusableSinks extends StreamComponents {
  val global: scala.reflect.api.Universe
  import global._

  trait UnusableSinkBase extends StreamSink
  {
    override def describe = None

    override def lambdaCount = 0

    override def subTrees = Nil

    override def outputNeeds = Set()

    override def emit(input: StreamInput, outputNeeds: OutputNeeds, nextOps: OpsAndOutputNeeds): StreamOutput =
      ???
  }

  /// Sink is explicitly invalid: a stream cannot end with it.
  case object InvalidSink extends UnusableSinkBase

  /// Sink that outputs a Unit (e.g. for a foreach).
  case object ScalarSink extends UnusableSinkBase
}
