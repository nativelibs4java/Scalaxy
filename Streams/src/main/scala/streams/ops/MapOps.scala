package scalaxy.streams

private[streams] trait MapOps
    extends ClosureStreamOps
    with CanBuildFromSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeMapOp {
    def unapply(tree: Tree): Option[(Tree, MapOp)] = Option(tree) collect {
      case q"$target.map[$_, $_](${Strip(Function(List(param), body))})($canBuildFrom)" =>
        (target, MapOp(param, body, canBuildFrom))
    }
  }

  case class MapOp(param: ValDef, body: Tree, canBuildFrom: Tree)
      extends ClosureStreamOp
  {
    override def describe = Some("map")

    override val sinkOption = Some(CanBuildFromSink(canBuildFrom))

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      val (replacedStatements, outputVars) =
        transformationClosure.replaceClosureBody(input, outputNeeds)

      val sub = emitSub(input.copy(vars = outputVars), nextOps)
      sub.copy(body = List(q"""
        ..$replacedStatements;
        ..${sub.body};
      """))
    }
  }
}
