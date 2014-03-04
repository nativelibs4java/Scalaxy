package scalaxy.loops

private[loops] trait FilterOps
    extends StreamSources
    with ClosureStreamOps
    with Strippers
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeFilterOp {
    def unapply(tree: Tree): Option[(Tree, FilterOp)] = Option(tree) collect {
      case q"$target.filter(${Strip(Function(List(param), body))})" =>
        (target, FilterOp(param, body))

      case q"$target.withFilter(${Strip(Function(List(param), body))})" =>
        (target, FilterOp(param, body))
    }
  }
  case class FilterOp(param: ValDef, body: Tree)
      extends ClosureStreamOp
  {
    override def sinkOption = None

    override def emitOp(
        inputVars: TuploidValue,
        outputNeeds: Set[TuploidPath],
        opsAndOutputNeeds: List[(StreamOp, Set[TuploidPath])],
        fresh: String => TermName,
        transform: Tree => Tree): StreamOpResult =
    {

      val (replacedStatements, outputVars) = transformationClosure.replaceClosureBody(inputVars, outputNeeds, fresh, transform)
      println("TODO: check this is Boolean: " + outputVars)

      val StreamOpResult(streamPrelude, streamBody, streamEnding) =
        emitSub(inputVars, opsAndOutputNeeds, fresh, transform)

      val test = outputVars.aliasName
      StreamOpResult(
        prelude = streamPrelude,
        // TODO match params and any tuple extraction in body with streamVars, replace symbols with streamVars values
        body = List(
          q"""
            ..$replacedStatements
            if ($test) {
              ..$streamBody
            }
          """
        ),
        ending = streamEnding
      )
    }
  }
}
