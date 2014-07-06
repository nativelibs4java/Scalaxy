package scalaxy.streams

private[streams] trait FilterOps extends ClosureStreamOps with Strippers
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeFilterOp {
    def unapply(tree: Tree): Option[(Tree, FilterOp)] = Option(tree) collect {
      case q"$target.filter(${Strip(Function(List(param), body))})" =>
        (target, FilterOp(param, body, false, "filter"))

      case q"$target.filterNot(${Strip(Function(List(param), body))})" =>
        (target, FilterOp(param, body, true, "filterNot"))

      case q"$target.withFilter(${Strip(Function(List(param), body))})" =>
        (target, FilterOp(param, body, false, "withFilter"))
    }
  }
  case class FilterOp(param: ValDef, body: Tree, isNegative: Boolean, name: String)
      extends ClosureStreamOp
  {
    override def describe = Some(name)

    override def sinkOption = None

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
      if (isNegative) {
        test = typed(q"!$test")
      }

      var sub = emitSub(input.copy(outputSize = None), nextOps)
      sub.copy(body = List(q"""
        ..$replacedStatements;
        if ($test) {
          ..${sub.body};
        }
      """))
    }
  }
}
