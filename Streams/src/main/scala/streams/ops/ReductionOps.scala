package scalaxy.streams

private[streams] trait ReductionOps
    extends StreamComponents
    with UnusableSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeReductionOp extends StreamOpExtractor {
    override def unapply(tree: Tree) = Option(tree) collect {
      case q"$target.sum[${tpt}](${_})" =>
        (target, SumOp(tpt.tpe))

      case q"$target.product[${tpt}](${_})" =>
        (target, ProductOp(tpt.tpe))
    }
  }

  trait SimpleReductorOp extends StreamOp
  {
    def opName: String
    override def describe = Some(opName)
    def throwsIfEmpty: Boolean
    def tpe: Type
    def initialAccumulatorValue: Tree
    def canAlterSize = true
    def accumulate(accumulator: Tree, newValue: Tree): Tree

    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) =
      Set(RootTuploidPath)

    override def lambdaCount = 0
    override def sinkOption = Some(ScalarSink)

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      val List((ScalarSink, _)) = nextOps

      import input._

      // requireSinkInput(input, outputNeeds, nextOps)

      val result = fresh(opName)
      val empty = fresh("empty")
      require(input.vars.alias.nonEmpty, s"input.vars = $input.vars")

      // println("inputVars.alias.get = " + inputVars.alias.get + ": " + inputVars.tpe)
      val emptyMessage = s"empty.$opName"
      val Block(List(
          resultDef,
          emptyDef,
          resultAdd,
          throwIfEmpty), resultRef) = typed(q"""
        ${newVar(result, tpe, initialAccumulatorValue)};
        private[this] var $empty = true;
        $result = ${accumulate(Ident(result), input.vars.alias.get)};
        if ($empty) throw new UnsupportedOperationException($emptyMessage);
        $result
      """)

      if (throwsIfEmpty)
        StreamOutput(
          prelude = List(resultDef, emptyDef),
          body = List(resultAdd),
          ending = List(throwIfEmpty, resultRef))
      else
        StreamOutput(
          prelude = List(resultDef),
          body = List(resultAdd),
          ending = List(resultRef))
    }
  }

  case class SumOp(tpe: Type) extends SimpleReductorOp
  {
    override def opName = "sum"
    override def initialAccumulatorValue = q"0"
    override def throwsIfEmpty = false
    override def subTrees = Nil
    override def accumulate(accumulator: Tree, newValue: Tree): Tree =
      q"$accumulator + $newValue"
  }

  case class ProductOp(tpe: Type) extends SimpleReductorOp
  {
    override def opName = "product"
    override def initialAccumulatorValue = q"1"
    override def throwsIfEmpty = false
    override def subTrees = Nil
    override def accumulate(accumulator: Tree, newValue: Tree): Tree =
      q"$accumulator * $newValue"
  }
}
