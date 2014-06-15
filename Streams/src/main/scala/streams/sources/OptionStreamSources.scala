package scalaxy.streams

private[streams] trait OptionStreamSources
    extends OptionSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeOptionStreamSource {
    // Testing the type would be so much better, but yields an awkward MissingRequirementError.
    // lazy val OptionTpe = typeOf[Option[_]]
    private[this] lazy val OptionSym = rootMirror.staticClass("scala.Option");
    private[this] lazy val SomeSym = rootMirror.staticClass("scala.Some");

    def hasOptionType(tree: Tree): Boolean = {
      val tpe = tree.tpe

      tpe != null && tpe != NoType &&
      (tpe.typeSymbol == OptionSym || tpe.typeSymbol == SomeSym)
    }

    def unapply(tree: Tree): Option[StreamSource] = Option(tree).filter(hasOptionType(_)) collect {
      case q"scala.Option.apply[${_}]($value)" =>
        InlineOptionStreamSource(value, isSome = false)

      case q"scala.Some.apply[${_}]($value)" =>
        InlineOptionStreamSource(value, isSome = true)

      case _ =>
        GenericOptionStreamSource(tree)
    }
  }

  case class GenericOptionStreamSource(
      option: Tree,
      sinkOption: Option[StreamSink] = Some(OptionSink))
    extends StreamSource
  {
    override def describe = Some("Option")

    override def lambdaCount = 0

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input.{ fresh, transform, typed }

      val optionVal = fresh("option")
      val itemVal = fresh("item")
      val nonEmptyVal = fresh("nonEmpty")

      // Early typing / symbolization.
      val Block(List(
          optionValDef,
          nonEmptyValDef,
          itemValDef,
          nonEmptyValRef,
          itemValRef), _) = typed(q"""
        private[this] val $optionVal = ${transform(option)};
        private[this] val $nonEmptyVal = $optionVal.nonEmpty;
        private[this] val $itemVal = $optionVal.get;
        $nonEmptyVal;
        $itemVal;
        ""
      """)
      val (extractionCode, outputVars) = createTuploidPathsExtractionDecls(itemValRef, outputNeeds, fresh, typed)

      val sub = emitSub(
        input.copy(
          vars = outputVars,
          outputSize = None), // TODO 1 if nonEmpty, 0 otherwise.
        nextOps)
      sub.copy(body = List(typed(q"""
        $optionValDef;
        $nonEmptyValDef;

        if ($nonEmptyValRef) {
          $itemValDef;
          ..$extractionCode
          ..${sub.body};
        }
      """)))
    }
  }

  case class InlineOptionStreamSource(
      item: Tree,
      isSome: Boolean,
      sinkOption: Option[StreamSink] = Some(OptionSink))
    extends StreamSource
  {
    override def describe = Some(if (isSome) "Some" else "Option")

    override def lambdaCount = 0

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input.{ fresh, transform, typed }

      val itemVal = fresh("item")
      val nonEmptyVal = fresh("nonEmpty")

      val nonEmptyTest =
        if (isSome || input.vars.tpe <:< typeOf[AnyVal])
          q"true"
        else
          q"$itemVal != null"

      // Early typing / symbolization.
      val Block(List(
          itemValDef,
          nonEmptyValDef,
          nonEmptyValRef,
          itemValRef), _) = typed(q"""
        private[this] val $itemVal = ${transform(item)};
        private[this] val $nonEmptyVal = $nonEmptyTest;
        $nonEmptyVal;
        $itemVal;
        ""
      """)
      val (extractionCode, outputVars) = createTuploidPathsExtractionDecls(itemValRef, outputNeeds, fresh, typed)

      val sub = emitSub(
        input.copy(
          vars = outputVars,
          outputSize = None), // TODO 1 if nonEmpty, 0 otherwise.
        nextOps)
      sub.copy(
        beforeBody = Nil,
        body = List(typed(q"""
          $itemValDef;
          $nonEmptyValDef;
          ..${sub.beforeBody};
          if ($nonEmptyValRef) {
            ..$extractionCode
            ..${sub.body};
          }
        """))
      )
    }
  }
}
