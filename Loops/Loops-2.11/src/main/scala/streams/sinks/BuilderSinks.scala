package scalaxy.loops

private[loops] trait BuilderSinks extends StreamComponents {
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

      // println("inputVars.alias.get = " + inputVars.alias.get + ": " + inputVars.tpe)
      val Block(List(builderDef, builderAdd, result), _) = typed(q"""
        private[this] val $builder = ${createBuilder(inputVars)};
        $builder += ${inputVars.alias.get};
        $builder.result();
        {}
      """)

      StreamOpResult(
        prelude = List(builderDef),
        body = List(builderAdd),
        ending = List(result))
    }
  }
}
