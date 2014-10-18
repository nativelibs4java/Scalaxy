package scalaxy.streams

private[streams] trait MapOps
    extends ClosureStreamOps
    with CanBuildFromSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeMapOp extends StreamOpExtractor {
    override def unapply(tree: Tree) = Option(tree) collect {
      case q"$target.map[${_}, ${_}](${Closure(closure)})($canBuildFrom)" =>
        (target, MapOp(closure, canBuildFrom = Some(canBuildFrom)))

      // Option.map doesn't take a CanBuildFrom.
      case q"$target.map[${_}](${Closure(closure)})" =>
        (target, MapOp(closure, canBuildFrom = None))
    }
  }

  case class MapOp(closure: Function, canBuildFrom: Option[Tree])
      extends ClosureStreamOp
  {
    override def describe = Some("map")

    override val sinkOption = canBuildFrom.map(CanBuildFromSink(_))

    override def canAlterSize = false

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      val (replacedStatements, outputVars) =
        transformationClosure.replaceClosureBody(input, outputNeeds)

      val sub = emitSub(input.copy(vars = outputVars), nextOps)
      sub.copy(body = replacedStatements ++ sub.body)
    }
  }
}
