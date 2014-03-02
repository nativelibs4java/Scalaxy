package scalaxy.loops

private[loops] trait CanBuildFromSinks extends StreamSources {
  val global: scala.reflect.api.Universe
  import global._

  trait CanBuildFromSink extends StreamSink {

    def canBuildFrom: Tree

    override def emitSink(
        streamVars: StreamVars,
        fresh: String => TermName,
        transform: Tree => Tree): StreamOpResult =
    {
      val builder = fresh("builder")
      StreamOpResult(
        // TODO pass source collection to canBuildFrom if it exists.
        prelude = List(q"val $builder = $canBuildFrom()"),
        body = List(q"$builder += ${streamVars.valueName}"),
        ending = List(q"$builder.result()")
      )
    }
  }
}
