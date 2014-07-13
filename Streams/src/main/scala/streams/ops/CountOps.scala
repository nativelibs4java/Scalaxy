package scalaxy.streams

private[streams] trait CountOps
    extends ClosureStreamOps
    with Strippers
    with OptionSinks
    with UnusableSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeCountOp extends StreamOpExtractor {
    override def unapply(tree: Tree) = Option(tree) collect {
      case q"$target.count(${Strip(Function(List(param), body))})" =>
        (target, CountOp(param, body))
    }
  }

  case class CountOp(param: ValDef, body: Tree) extends ClosureStreamOp {
    override def canInterruptLoop = false
    override def canAlterSize = true
    override def isMapLike = false
    override def sinkOption = Some(ScalarSink)
    override def describe = Some("count")

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      val List((ScalarSink, _)) = nextOps

      import input.{ typed, fresh }

      val (replacedStatements, outputVars) =
        transformationClosure.replaceClosureBody(
          input,
          outputNeeds + RootTuploidPath)

      var test = outputVars.alias.get

      val count = fresh("count")

      // Force typing of declarations and get typed references to various vars and vals.
      val Block(List(
          countVarDef,
          countIncr,
          countVarRef), _) = typed(q"""
        private[this] var $count = 0;
        $count += 1;
        $count;
        ""
      """)

      StreamOutput(
        prelude = List(countVarDef),
        body = List(q"""
          ..$replacedStatements;
          if ($test) {
            $countIncr;
          }
        """),
        ending = List(countVarRef))
    }
  }
}
