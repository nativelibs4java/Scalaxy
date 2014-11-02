package scalaxy.streams

private[streams] trait IsEmptyOps
    extends UnusableSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeIsEmptyOp extends StreamOpExtractor {
    override def unapply(tree: Tree) = Option(tree) collect {
      case q"$target.isEmpty" =>
        (target, IsEmptyOp("isEmpty", true))

      case q"$target.isDefined" =>
        (target, IsEmptyOp("isDefined", false))

      case q"$target.nonEmpty" =>
        (target, IsEmptyOp("nonEmpty", false))
    }
  }

  case class IsEmptyOp(name: String, isPositivelyEmpty: Boolean) extends StreamOp {
    override def lambdaCount = 0
    override def sinkOption = Some(ScalarSink)
    override def describe = Some(name)
    override def canInterruptLoop = true
    override def canAlterSize = true
    override def subTrees = Nil
    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) =
      Set() // TODO: check this.

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      val List((ScalarSink, _)) = nextOps

      import input.{ typed, fresh }

      val isEmpty = fresh("isEmpty")

      // Force typing of declarations and get typed references to various vars and vals.
      val Block(List(
          isEmptyVarDef,
          isEmptyIsFalse), result) = typed(q"""
        private[this] var $isEmpty = true;
        $isEmpty = false;
        ${if (isPositivelyEmpty) q"$isEmpty" else q"!$isEmpty"}
      """)

      StreamOutput(
        prelude = List(isEmptyVarDef),
        body = List(q"""
          $isEmptyIsFalse;
          ${input.loopInterruptor.get.duplicate} = false;
        """),
        ending = List(result))
    }
  }
}
