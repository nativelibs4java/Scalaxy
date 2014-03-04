package scalaxy.loops

private[loops] trait CanBuildFromSinks extends StreamSources {
  val global: scala.reflect.api.Universe
  import global._

  case class CanBuildFromSink(canBuildFrom: Tree) extends StreamSink
  {
    override def sinkOption = Some(this)

    override def outputNeeds = Set(RootTuploidPath)

    override def emitSink(
        inputVars: TuploidValue,
        fresh: String => TermName,
        transform: Tree => Tree): StreamOpResult =
    {
      val builder = fresh("builder")
      require(inputVars.aliasName.toString != "", s"inputVars = $inputVars")

      StreamOpResult(
        // TODO pass source collection to canBuildFrom if it exists.
        prelude = List(q"val $builder = $canBuildFrom()"),
        body = List(q"$builder += ${inputVars.aliasName}"),
        ending = List(q"$builder.result()")
      )
    }
  }
}
