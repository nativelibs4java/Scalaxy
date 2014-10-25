package scalaxy.streams

private[streams] trait WhileOps
    extends ClosureStreamOps
    with Strippers
    with OptionSinks
    with UnusableSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeWhileOp extends StreamOpExtractor {
    def sinkOptionForReturnType(tpe: Type) =
      if (tpe <:< typeOf[Range])
        Some(InvalidSink)
      else
        None

    override def unapply(tree: Tree) = Option(tree) collect {
      case q"$target.takeWhile(${Closure(closure)})" =>
        (target, TakeWhileOp(closure, sinkOptionForReturnType(tree.tpe)))

      case q"$target.dropWhile(${Closure(closure)})" =>
        (target, DropWhileOp(closure, sinkOptionForReturnType(tree.tpe)))
    }
  }

  trait WhileOp extends ClosureStreamOp {
    // override def sinkOption = None
    override def canAlterSize = true
    override def isMapLike = false
  }

  case class TakeWhileOp(closure: Function, sinkOption: Option[StreamSink]) extends WhileOp
  {
    override def canInterruptLoop = true
    override def describe = Some("takeWhile")

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
        } else {
          ${input.loopInterruptor.get.duplicate} = false;
        }
      """))
    }
  }

  case class DropWhileOp(closure: Function, sinkOption: Option[StreamSink]) extends WhileOp
  {
    override def canInterruptLoop = false
    override def describe = Some("dropWhile")

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input.{ typed, fresh }

      val (replacedStatements, outputVars) =
        transformationClosure.replaceClosureBody(
          input,
          outputNeeds + RootTuploidPath)

      val test = outputVars.alias.get

      val doneDropping = fresh("doneDropping")
      // Force typing of declarations and get typed references to various vars and vals.
      val Block(List(
          doneDroppingVarDef,
          setDoneDropping), combinedTest) = typed(q"""
        private[this] var $doneDropping = false;
        $doneDropping = true;
        $doneDropping || !$test
      """)

      val sub = emitSub(input.copy(outputSize = None), nextOps)
      sub.copy(
        beforeBody = sub.beforeBody :+ doneDroppingVarDef,
        body = List(q"""
        ..$replacedStatements;
        if ($combinedTest) {
          $setDoneDropping;
          ..${sub.body};
        }
      """))
    }
  }
}
