package scalaxy.streams

private[streams] trait OptionSinks extends StreamComponents {
  val global: scala.reflect.api.Universe
  import global._

  case object OptionSink extends StreamSink 
  {
    override def lambdaCount = 0

    override def subTrees = Nil

    override def describe = Some("Option")

    override def emit(input: StreamInput, outputNeeds: OutputNeeds, nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input._

      requireSinkInput(input, outputNeeds, nextOps)

      val value = fresh("value")
      val nonEmpty = fresh("nonEmpty")
      require(input.vars.alias.nonEmpty, s"input.vars = $input.vars")

      val ntpe = normalize(input.vars.tpe)
      val Block(List(
          valueDef,
          nonEmptyDef,
          assignment), result) = typed(q"""
        private[this] var $value: $ntpe = null.asInstanceOf[$ntpe];
        private[this] var $nonEmpty = false;
        {
          $value = ${input.vars.alias.get};
          $nonEmpty = true;
        };
        if ($nonEmpty) Some($value) else None
      """)

      StreamOutput(
        prelude = List(valueDef, nonEmptyDef),
        body = List(assignment),
        ending = List(result))
    }
  }
}
