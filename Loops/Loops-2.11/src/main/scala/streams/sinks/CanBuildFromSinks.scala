package scalaxy.loops

private[loops] trait CanBuildFromSinks extends StreamSources {
  val global: scala.reflect.api.Universe
  import global._

  case class CanBuildFromSink(canBuildFrom: Tree) extends StreamSink
  {
    override def sinkOption = Some(this)

    override def outputNeeds = Set(RootTuploidPath)

    override def emitSink(
        inputVars: TuploidValue[Tree],
        fresh: String => TermName,
        transform: Tree => Tree): StreamOpResult =
    {
      val builder = fresh("builder")
      require(inputVars.alias.nonEmpty, s"inputVars = $inputVars")

      val Block(List(builderDef, builderRef), _) = typed(q"""
        private[this] val $builder = $canBuildFrom();
        $builder;
        {}
      """)

      StreamOpResult(
        // TODO pass source collection to canBuildFrom if it exists.
        prelude = List(builderDef),
        body = List(q"$builderRef += ${inputVars.alias.get}"),
        ending = List(q"$builderRef.result()")
      )
    }
  }
}
