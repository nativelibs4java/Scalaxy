package scalaxy.streams

private[streams] trait OptionSinks extends StreamComponents {
  val global: scala.reflect.api.Universe
  import global._

  // Base class for builder-based sinks.
  case object OptionSink extends StreamSink
  {
    override def describe = Some("Option")

    override def lambdaCount = 0

    override def emit(input: StreamInput, outputNeeds: OutputNeeds, nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input._

      requireSinkInput(input, outputNeeds, nextOps)

      val value = fresh("value")
      val nonEmpty = fresh("nonEmpty")
      require(input.vars.alias.nonEmpty, s"input.vars = $input.vars")

      val tpe = input.vars.tpe
      val Block(List(
          valueDef,
          nonEmptyDef,
          assignment,
          result), _) = typed(q"""
        private[this] var $value: $tpe = ${Literal(Constant(defaultValue(input.vars.tpe)))};
        private[this] var $nonEmpty = false;
        {
          $value = ${input.vars.alias.get};
          $nonEmpty = true;
        };
        if ($nonEmpty) Some($value) else None;
        ""
      """)

      StreamOutput(
        prelude = List(valueDef, nonEmptyDef),
        body = List(assignment),
        ending = List(result))
    }
  }
}
