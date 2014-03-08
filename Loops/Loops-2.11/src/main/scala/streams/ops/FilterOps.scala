package scalaxy.loops

private[loops] trait FilterOps extends ClosureStreamOps
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

    override def emitOp(
        inputVars: TuploidValue[Tree],
        outputNeeds: Set[TuploidPath],
        opsAndOutputNeeds: List[(StreamOp, Set[TuploidPath])],
        fresh: String => TermName,
        transform: Tree => Tree): StreamOpResult =
    {

      val (replacedStatements, outputVars) =
        transformationClosure.replaceClosureBody(
          inputVars, outputNeeds + RootTuploidPath, fresh, transform)

      val StreamOpResult(streamPrelude, streamBody, streamEnding) =
        emitSub(inputVars, opsAndOutputNeeds, fresh, transform)

      var test = outputVars.alias.get
      if (isNegative) {
        test = typed(q"!$test")
      }
      StreamOpResult(
        prelude = streamPrelude,
        body = List(
          q"""
            ..$replacedStatements;
            if ($test) {
              ..$streamBody;
            }
          """
        ),
        ending = streamEnding
      )
    }
  }
}
