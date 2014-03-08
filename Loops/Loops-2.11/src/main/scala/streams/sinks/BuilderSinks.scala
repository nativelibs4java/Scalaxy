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

      val Block(List(builderDef, builderAdd, result), _) = typed(q"""
        private[this] val $builder = ${createBuilder(inputVars)};
        $builder += ${inputVars.alias.get};
        $builder.result();
        {}
      """)

      StreamOpResult(
        prelude = List(builderDef),
        body = List(builderAdd),
        ending = List(result)
      )
    }
  }

  case object ArrayBuilderSink extends BuilderSink
  {
    // TODO build array of same size as source collection if it is known.
    override def createBuilder(inputVars: TuploidValue[Tree]) =
      q"scala.collection.mutable.ArrayBuilder[${inputVars.tpe}]()"
  }

  case object ListBufferSink extends BuilderSink
  {
    override def createBuilder(inputVars: TuploidValue[Tree]) =
      q"scala.collection.mutable.ListBuffer[${inputVars.tpe}]()"
  }

  case object SetBuilderSink extends BuilderSink
  {
    override def createBuilder(inputVars: TuploidValue[Tree]) =
      q"scala.collection.mutable.SetBuilder[${inputVars.tpe}]()"
  }
}
