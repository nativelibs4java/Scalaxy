package scalaxy.loops

private[loops] trait BuilderSinks extends StreamSources {
  val global: scala.reflect.api.Universe
  import global._

  trait BuilderSink extends StreamSink
  {
    def createBuilder(inputVars: TuploidValue[Tree]): Tree

    override def emitSink(
        inputVars: TuploidValue[Tree],
        fresh: String => TermName,
        transform: Tree => Tree): StreamOpResult =
    {
      val builder = fresh("builder")
      require(inputVars.alias.nonEmpty, s"inputVars = $inputVars")

      // TODO pass source collection to the builder if it exists.
      val Block(List(builderDef, builderRef), _) = typed(q"""
        private[this] val $builder = ${createBuilder(inputVars)};
        $builder;
        {}
      """)

      StreamOpResult(
        prelude = List(builderDef),
        body = List(typed(q"$builderRef += ${inputVars.alias.get}")),
        ending = List(typed(q"$builderRef.result()"))
      )
    }
  }
}
