package scalaxy.streams

private[streams] trait FindOps extends ClosureStreamOps with Strippers with OptionSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeFindOp {
    def unapply(tree: Tree): Option[(Tree, FindOp)] = Option(tree) collect {
      case q"$target.find(${Strip(Function(List(param), body))})" =>
        (target, FindOp(param, body))
    }
  }
  case class FindOp(param: ValDef, body: Tree)
      extends ClosureStreamOp
  {
    override def describe = Some("find")

    override def sinkOption = Some(OptionSink)

    override def canInterruptLoop = true

    override def isMapLike = false

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input.typed

      val (replacedStatements, outputVars) =
        transformationClosure.replaceClosureBody(
          input,
          outputNeeds + RootTuploidPath)

      var test = outputVars.alias.get

      var sub = emitSub(input.copy(outputSize = None), nextOps)
      sub.copy(body = List(q"""
        ..$replacedStatements;
        if ($test) {
          ..${sub.body};
          ${input.loopInterruptor.get.duplicate} = false;
        }
      """))
    }
  }
}
