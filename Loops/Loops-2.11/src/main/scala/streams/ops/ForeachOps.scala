package scalaxy.loops

private[loops] trait ForeachOps
    extends StreamSources
    with ClosureStreamOps
    with Strippers
    with UnusableSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeForeachOp {
    def unapply(tree: Tree): Option[(Tree, ForeachOp)] = Option(tree) collect {
      case q"$target.foreach[$_](${Strip(Function(List(param), body))})" =>
        (target, ForeachOp(param, body))
    }
  }
  case class ForeachOp(param: ValDef, body: Tree)
      extends ClosureStreamOp
  {
    override def sinkOption = Some(UnusableSink)

    override def emitOp(
        inputVars: TuploidValue[Tree],
        outputNeeds: Set[TuploidPath],
        opsAndOutputNeeds: List[(StreamOp, Set[TuploidPath])],
        fresh: String => TermName,
        transform: Tree => Tree): StreamOpResult =
    {
      val List((SinkOp(UnusableSink), _)) = opsAndOutputNeeds

      val (replacedStatements, outputVars) = transformationClosure.replaceClosureBody(inputVars, outputNeeds, fresh, transform)

      require(outputVars.tpe =:= typeOf[Unit], "Expected Unit, got " + outputVars.tpe)

      // println(s"""
      //   body: $body,
      //   transformationClosure: $transformationClosure,
      //   statements = ${transformationClosure.statements},
      //   replacedStatements = $replacedStatements
      // """)

      StreamOpResult(
        prelude = Nil,
        // TODO match params and any tuple extraction in body with streamVars, replace symbols with streamVars values
        body = replacedStatements,
        ending = Nil)
    }
  }
}
