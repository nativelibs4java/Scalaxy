package scalaxy.streams

private[streams] trait OptionOps
    extends UnusableSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeOptionOp extends StreamOpExtractor {
    override def unapply(tree: Tree) = Option(tree) collect {
      case q"$target.get" =>
        (target, OptionGetOrElseOp("get", q"""
          throw new NoSuchElementException("None.get")
        """))

      case q"$target.getOrElse[${_}]($v)" =>
        (target, OptionGetOrElseOp("getOrElse", v))

      case q"$target.isEmpty" =>
        (target, OptionIsEmptyOp)
    }
  }

  trait OptionOpBase extends StreamOp
  {
    override def lambdaCount = 0

    override def subTrees = Nil

    def whenSome(value: Tree): Tree
    def whenNone(): Tree

    override def emit(input: StreamInput, outputNeeds: OutputNeeds, nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input._

      // TODO: remove this restriction to unlock flatMap.
      val List((ScalarSink, _)) = nextOps

      val value = fresh("value")
      val nonEmpty = fresh("nonEmpty")
      require(input.vars.alias.nonEmpty, s"input.vars = $input.vars")

      val tpe = input.vars.tpe
      val Block(List(
          valueDef,
          nonEmptyDef,
          assignment), result) = typed(q"""
        private[this] var $value: $tpe =
          ${Literal(Constant(defaultValue(input.vars.tpe)))};
        private[this] var $nonEmpty = false;
        {
          $value = ${input.vars.alias.get};
          $nonEmpty = true;
        };
        if ($nonEmpty) ${whenSome(q"$value")} else ${whenNone}
      """)

      // println(s"result = $result")

      StreamOutput(
        prelude = List(valueDef, nonEmptyDef),
        body = List(assignment),
        ending = List(result))
    }
  }

  case class OptionGetOrElseOp(name: String, defaultValue: Tree) extends OptionOpBase {
    override def sinkOption = Some(ScalarSink)
    override def canAlterSize = true
    override def describe = Some(name)
    override def whenSome(value: Tree) = value
    override def whenNone() = defaultValue
    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) =
      Set(RootTuploidPath) // TODO: refine this.
  }

  case object OptionIsEmptyOp extends OptionOpBase {
    override def sinkOption = Some(ScalarSink)
    override def canAlterSize = true
    override def describe = Some("isEmpty")
    override def whenSome(value: Tree) = q"false"
    override def whenNone() = q"true"
    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) =
      Set() // TODO: check this.
  }
}
