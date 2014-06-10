package scalaxy.streams

private[streams] trait ReductionSinks extends StreamComponents {
  val global: scala.reflect.api.Universe
  import global._

  // Base class for builder-based sinks.
  case class SumSink(tpe: Type) extends StreamSink
  {
    override def describe = Some("sum")

    override def lambdaCount = 0

    override def emit(input: StreamInput, outputNeeds: OutputNeeds, nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input._

      requireSinkInput(input, outputNeeds, nextOps)

      val total = fresh("total")
      require(input.vars.alias.nonEmpty, s"input.vars = $input.vars")

      // println("inputVars.alias.get = " + inputVars.alias.get + ": " + inputVars.tpe)
      val Block(List(
          totalDef,
          totalAdd,
          result), _) = typed(q"""
        private[this] var $total: $tpe = 0;
        $total =  $total + ${input.vars.alias.get};
        $total;
        ""
      """)

      StreamOutput(
        prelude = List(totalDef),
        body = List(totalAdd),
        ending = List(result))
    }
  }
}
