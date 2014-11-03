package scalaxy.streams

private[streams] trait OptionOps
    extends UnusableSinks
    with OptionSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeOptionOp extends StreamOpExtractor {
    override def unapply(tree: Tree) = Option(tree) collect {
      case q"$target.get" =>
        (target, OptionGetOrElseOp("get", q"""
          throw new NoSuchElementException("None.get")
        """))

      case q"$target.orNull[${_}](${_})" =>
        (target, OptionGetOrElseOp("orNull", q"null"))

      case q"$target.getOrElse[${_}]($v)" =>
        (target, OptionGetOrElseOp("getOrElse", v))

      // case q"$target.orElse[${_}]($v)" =>
      //   (target, OptionOrElseOp(v))
    }
  }

  case class OptionGetOrElseOp(name: String, defaultValue: Tree) extends StreamOp {
    override def lambdaCount = 1
    override def sinkOption = Some(ScalarSink)
    override def canAlterSize = true
    override def describe = Some(name)
    override def subTrees = Nil
    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) =
      Set(RootTuploidPath) // TODO: refine this.

    override def emit(input: StreamInput, outputNeeds: OutputNeeds, nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input._

      // TODO: remove this to unlock flatMap
      val List((ScalarSink, _)) = nextOps

      val value = fresh("value")
      val nonEmpty = fresh("nonEmpty")
      require(input.vars.alias.nonEmpty, s"input.vars = $input.vars")

      val Block(List(
          valueDef,
          nonEmptyDef,
          assignment), result) = typed(q"""
        ${newVar(value, input.vars.tpe)};
        private[this] var $nonEmpty = false;
        {
          $value = ${input.vars.alias.get};
          $nonEmpty = true;
        };
        if ($nonEmpty) $value else $defaultValue
      """)

      StreamOutput(
        prelude = List(valueDef, nonEmptyDef),
        body = List(assignment),
        ending = List(result))
    }
  }

  // case class OptionOrElseOp(defaultValue: Tree) extends StreamOp {
  //   override def lambdaCount = 1
  //   override def sinkOption = Some(OptionSink)
  //   override def canAlterSize = true
  //   override def describe = Some("orElse")
  //   override def subTrees = Nil
  //   override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) =
  //     Set(RootTuploidPath) // TODO: refine this.

  //   override def emit(input: StreamInput, outputNeeds: OutputNeeds, nextOps: OpsAndOutputNeeds): StreamOutput =
  //   {
  //     import input._

  //     val value = fresh("value")
  //     val nonEmpty = fresh("nonEmpty")
  //     require(input.vars.alias.nonEmpty, s"input.vars = $input.vars")

  //     val Block(List(
  //         valueDef,
  //         nonEmptyDef,
  //         assignment),
  //         TupleCreation(List(
  //           valueRef, nonEmptyRef))) = typed(q"""
  //       ${newVar(value, input.vars.tpe)};
  //       private[this] var $nonEmpty = false;
  //       {
  //         $value = ${input.vars.alias.get};
  //         $nonEmpty = true;
  //       };
  //       (
  //         $value,
  //         $nonEmpty
  //       )
  //     """)

  //     val flatSink = FlattenedOptionSink(valueRef, nonEmptyRef)

  //     val sub = emitSub(
  //       input.copy(
  //         vars = ScalarValue(input.vars.tpe, alias = Some(valueRef)),
  //         outputSize = None,
  //         index = None),
  //       nextOps)
  //     // ..${sub.body.map(untyped)};
  //     sub.copy(
  //       // beforeBody = sub.beforeBody,
  //       body = List(q"""
  //         $valueDef;
  //         $nonEmptyDef;
  //         if ($nonEmptyRef) {
  //           $assignment;
  //           ..${sub.body};
  //         }
  //       """))
  //   }
  // }
}
