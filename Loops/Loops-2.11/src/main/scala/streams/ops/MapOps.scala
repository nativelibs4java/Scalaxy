package scalaxy.loops

private[loops] trait MapOps
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

    override def emitOp(
        inputVars: TuploidValue[Tree],
        outputNeeds: Set[TuploidPath],
        opsAndOutputNeeds: List[(StreamOp, Set[TuploidPath])],
        fresh: String => TermName,
        transform: Tree => Tree): StreamOpOutput =
    {
      val (replacedStatements, outputVars) = transformationClosure.replaceClosureBody(inputVars, outputNeeds, fresh, transform)
      val StreamOpOutput(streamPrelude, streamBody, streamEnding) =
        emitSub(outputVars, opsAndOutputNeeds, fresh, transform)

      StreamOpOutput(
        prelude = streamPrelude,
        body = List(q"""
          ..$replacedStatements;
          ..$streamBody;
        """),
        ending = streamEnding
      )
    }
  }
}
