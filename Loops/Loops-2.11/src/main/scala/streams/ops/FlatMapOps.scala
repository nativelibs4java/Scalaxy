package scalaxy.loops

private[loops] trait FlatMapOps
    extends StreamSources
    with CanBuildFromSinks
    with TuploidValues
    with TransformationClosures
    with ClosureStreamOps
    with Strippers
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeFlatMapOp {
    def unapply(tree: Tree): Option[(Tree, FlatMapOp)] = Option(tree) collect {
      case q"$target.flatMap[$_, $_](${Strip(Function(List(param), body))})($canBuildFrom)" =>
        (target, FlatMapOp(param, body, canBuildFrom))
    }
  }

  case class FlatMapOp(param: ValDef, body: Tree, canBuildFrom: Tree)
      extends ClosureStreamOp
  {
    override val sinkOption = Some(CanBuildFromSink(canBuildFrom))

    override def emitOp(
        inputVars: TuploidValue[TermName],
        outputNeeds: Set[TuploidPath],
        opsAndOutputNeeds: List[(StreamOp, Set[TuploidPath])],
        fresh: String => TermName,
        transform: Tree => Tree): StreamOpResult =
    {
      ???
    }
  }
}
