package scalaxy.streams

private[streams] trait ReductionSinks extends StreamComponents {
  val global: scala.reflect.api.Universe
  import global._

  trait SimpleReductorSink extends StreamSink
  {
  	def tpe: Type
  	def initialAccumulatorValue: Tree
  	def accumulate(accumulator: Tree, newValue: Tree): Tree

    override def lambdaCount = 0

    override def emit(input: StreamInput, outputNeeds: OutputNeeds, nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input._

      requireSinkInput(input, outputNeeds, nextOps)

      val result = fresh("result")
      require(input.vars.alias.nonEmpty, s"input.vars = $input.vars")

      // println("inputVars.alias.get = " + inputVars.alias.get + ": " + inputVars.tpe)
      val Block(List(
          resultDef,
          resultAdd,
          resultRef), _) = typed(q"""
        private[this] var $result: $tpe = ${initialAccumulatorValue};
        $result = ${accumulate(Ident(result), input.vars.alias.get)};
        $result;
        ""
      """)

      StreamOutput(
        prelude = List(resultDef),
        body = List(resultAdd),
        ending = List(resultRef))
    }
  }

  case class SumSink(tpe: Type) extends SimpleReductorSink
  {
    override def describe = Some("sum")
    override def initialAccumulatorValue = q"0"
  	override def accumulate(accumulator: Tree, newValue: Tree): Tree =
  		q"$accumulator + $newValue"
  }

  case class ProductSink(tpe: Type) extends SimpleReductorSink
  {
    override def describe = Some("product")
    override def initialAccumulatorValue = q"1"
  	override def accumulate(accumulator: Tree, newValue: Tree): Tree =
  		q"$accumulator * $newValue"
  }
}
