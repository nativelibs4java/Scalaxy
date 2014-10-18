package scalaxy.streams

private[streams] trait ForeachOps
    extends ClosureStreamOps
    with UnusableSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeForeachOp extends StreamOpExtractor {
    override def unapply(tree: Tree) = Option(tree) collect {
      case q"$target.foreach[${_}](${Closure(closure)})" =>
        (target, ForeachOp(closure))
    }
  }
  case class ForeachOp(closure: Function)
      extends ClosureStreamOp
  {
    override def describe = Some("foreach")

    override def sinkOption = Some(ScalarSink)

    /// Technically, the output size of the Unit output is zero, so it's altered.
    override def canAlterSize = true

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      val List((ScalarSink, _)) = nextOps

      val (replacedStatements, outputVars) =
        transformationClosure.replaceClosureBody(input, outputNeeds)

      // require(outputVars.tpe.dealias =:= typeOf[Unit], "Expected Unit, got " + outputVars.tpe)

      StreamOutput(body = replacedStatements)
    }
  }
}
