package scalaxy.streams

private[streams] trait ExistsOps
    extends ClosureStreamOps
    with Strippers
    with OptionSinks
    with UnusableSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeExistsOp extends StreamOpExtractor {
    override def unapply(tree: Tree) = Option(tree) collect {
      case q"$target.exists(${Closure(closure)})" =>
        (target, ExistsOp(closure))

      case q"$target.forall(${Closure(closure)})" =>
        (target, ForallOp(closure))
    }
  }

  case class ExistsOp(override val closure: Function)
      extends ExistsOpLike("exists", exists = true, closure)

  case class ForallOp(override val closure: Function)
      extends ExistsOpLike("forall", exists = false, closure)

  class ExistsOpLike(name: String, exists: Boolean, val closure: Function) extends ClosureStreamOp {
    override def canInterruptLoop = true
    override def canAlterSize = true
    override def isMapLike = false
    override def sinkOption = Some(ScalarSink)
    override def describe = Some(name)

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

      val result = fresh("result")

      // Force typing of declarations and get typed references to various vars and vals.
      val Block(List(
          resultVarDef,
          resultFalse,
          resultTrue,
          resultVarRef), _) = typed(q"""
        private[this] var $result = ${if (exists) q"false" else q"true"};
        $result = false;
        $result = true;
        $result;
        ""
      """)

      StreamOutput(
        prelude = List(resultVarDef),
        body = List(q"""
          ..$replacedStatements;
          if (${if (exists) test else q"!$test"}) {
            ${if (exists) resultTrue else resultFalse};
            ${input.loopInterruptor.get.duplicate} = false;
          }
        """),
        ending = List(resultVarRef))
    }
  }
}
