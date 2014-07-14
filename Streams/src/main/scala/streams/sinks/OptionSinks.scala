package scalaxy.streams

private[streams] trait OptionSinks extends StreamComponents {
  val global: scala.reflect.api.Universe
  import global._

  trait OptionSinkBase extends StreamSink
  {
    override def lambdaCount = 0

    def whenSome(value: Tree): Tree
    def whenNone(): Tree

    override def emit(input: StreamInput, outputNeeds: OutputNeeds, nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input._

      requireSinkInput(input, outputNeeds, nextOps)

      val value = fresh("value")
      val nonEmpty = fresh("nonEmpty")
      require(input.vars.alias.nonEmpty, s"input.vars = $input.vars")

      val tpe = input.vars.tpe
      val Block(List(
          valueDef,
          nonEmptyDef,
          assignment,
          result), _) = typed(q"""
        private[this] var $value: $tpe =
          ${Literal(Constant(defaultValue(input.vars.tpe)))};
        private[this] var $nonEmpty = false;
        {
          $value = ${input.vars.alias.get};
          $nonEmpty = true;
        };
        if ($nonEmpty) ${whenSome(q"$value")} else ${whenNone};
        ""
      """)

      StreamOutput(
        prelude = List(valueDef, nonEmptyDef),
        body = List(assignment),
        ending = List(result))
    }
  }

  case object OptionSink extends OptionSinkBase {
    override def describe = Some("Option")
    override def whenSome(value: Tree) = q"Some($value)"
    override def whenNone() = q"None"
  }

  case class OptionGetOrElseSink(defaultValue: Tree) extends OptionSinkBase {
    override def describe = None
    override def whenSome(value: Tree) = value
    override def whenNone() = defaultValue
  }

  case object OptionIsEmptySink extends OptionSinkBase {
    override def describe = None
    override def whenSome(value: Tree) = q"false"
    override def whenNone() = q"true"
  }
}
