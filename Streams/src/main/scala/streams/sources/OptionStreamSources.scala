package scalaxy.streams

private[streams] trait OptionStreamSources
    extends OptionSinks
    with StreamInterruptors
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeOptionStreamSource {
    // Testing the type would be so much better, but yields an awkward MissingRequirementError.
    // lazy val OptionTpe = typeOf[Option[_]]
    private[this] lazy val OptionClass = rootMirror.staticClass("scala.Option")
    private[this] lazy val OptionModule = rootMirror.staticModule("scala.Option")
    private[this] lazy val SomeClass = rootMirror.staticClass("scala.Some")
    private[this] lazy val SomeModule = rootMirror.staticModule("scala.Some")
    private[this] lazy val NoneClass = rootMirror.staticClass("scala.None")
    private[this] lazy val NoneModule = rootMirror.staticModule("scala.None")
    private[this] lazy val OptionClasses: Set[Symbol] =
      Set(OptionClass, SomeClass, NoneClass)

    def hasOptionType(tree: Tree): Boolean = tree.symbol == NoneModule || {
      val tpe = tree.tpe

      tpe != null && tpe != NoType && OptionClasses(tpe.typeSymbol)
    }

    private[this] def getSinkOption(tree: Tree) = {
      val TypeRef(_, _, List(componentTpe)) = tree.tpe.baseType(OptionClass)
      Some(OptionSink(componentTpe = Some(componentTpe)))
    }

    def unapply(tree: Tree): Option[StreamSource] = Option(tree).filter(hasOptionType(_)) collect {
      case q"$option.apply[$tpt]($value)" if option.symbol == OptionModule =>
        InlineOptionStreamSource(
          tpt.tpe,
          value,
          isSome = false,
          sinkOption = getSinkOption(tree))

      case q"$some.apply[$tpt]($value)" if some.symbol == SomeModule =>
        InlineOptionStreamSource(
          tpt.tpe,
          value,
          isSome = true,
          sinkOption = getSinkOption(tree))

      case _ =>
        GenericOptionStreamSource(
          tree,
          sinkOption = getSinkOption(tree))
    }
  }

  object OptionStreamSource {
    def unapply(source: StreamSource) = Option(source) collect {
      case GenericOptionStreamSource(_, _) | InlineOptionStreamSource(_, _, _, _) =>
        source
    }
  }

  case class GenericOptionStreamSource(
      option: Tree,
      sinkOption: Option[StreamSink])
    extends StreamSource
  {
    override def describe = Some("Option")

    override def lambdaCount = 0

    override def subTrees = List(option)

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
          itemValDef),
          TupleCreation(List(
            nonEmptyValRef, itemValRef))) = typed(q"""
        private[this] val $optionVal = ${transform(option)};
        private[this] val $nonEmptyVal = $optionVal.nonEmpty;
        private[this] val $itemVal = $optionVal.get;
        ($nonEmptyVal, $itemVal)
      """)
      val TuploidPathsExtractionDecls(extractionCode, outputVars, coercionSuccessVarDefRef) =
        createTuploidPathsExtractionDecls(
          itemValRef.tpe, itemValRef, outputNeeds, fresh, typed,
          newCoercionSuccessVarDefRef(nextOps, fresh, typed))

      // println(s"""
      //   outputNeeds: $outputNeeds
      //   extractionCode: $extractionCode
      //   outputVars: $outputVars
      //   coercionSuccessVarDefRef: $coercionSuccessVarDefRef
      // """)

      val interruptor = new StreamInterruptor(input, nextOps)
      val sub = emitSub(
        input.copy(
          vars = outputVars,
          loopInterruptor = interruptor.loopInterruptor,
          outputSize = None), // TODO 1 if nonEmpty, 0 otherwise.
        nextOps,
        coercionSuccessVarDefRef._2)
      sub.copy(body = List(typed(q"""
        $optionValDef;
        $nonEmptyValDef;
        ..${interruptor.defs}

        if (${interruptor.composeTest(nonEmptyValRef)}) {
          $itemValDef;
          ..$extractionCode
          ..${sub.body};
        }
      """)))
    }
  }

  case class InlineOptionStreamSource(
      tpe: Type,
      item: Tree,
      isSome: Boolean,
      sinkOption: Option[StreamSink])
    extends StreamSource
  {
    override def describe = Some(if (isSome) "Some" else "Option")

    override def lambdaCount = 0

    override def subTrees = List(item)

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input.{ fresh, transform, typed }

      val itemVal = fresh("item")
      val nonEmptyVal = fresh("nonEmpty")

      val nonEmptyTest =
        if (isSome || tpe <:< typeOf[AnyVal])
          q"true"
        else
          q"$itemVal != null"

      // Early typing / symbolization.
      val Block(List(
          itemValDef,
          nonEmptyValDef),
          TupleCreation(List(
            nonEmptyValRef, itemValRef))) = typed(q"""
        private[this] val $itemVal: $tpe = ${transform(item)};
        private[this] val $nonEmptyVal = $nonEmptyTest;
        ($nonEmptyVal, $itemVal)
      """)
      val TuploidPathsExtractionDecls(extractionCode, outputVars, coercionSuccessVarDefRef) =
        createTuploidPathsExtractionDecls(
          tpe, itemValRef, outputNeeds, fresh, typed,
          newCoercionSuccessVarDefRef(nextOps, fresh, typed))

      val interruptor = new StreamInterruptor(input, nextOps)
      val sub = emitSub(
        input.copy(
          vars = outputVars,
          loopInterruptor = interruptor.loopInterruptor,
          outputSize = None), // TODO 1 if nonEmpty, 0 otherwise.
        nextOps,
        coercionSuccessVarDefRef._2)
      sub.copy(
        beforeBody = Nil,
        body = List(typed(q"""
          $itemValDef;
          $nonEmptyValDef;
          ..${interruptor.defs}
          ..${sub.beforeBody};
          if (${interruptor.composeTest(nonEmptyValRef)}) {
            ..$extractionCode
            ..${sub.body};
          }
          ..${sub.afterBody}
        """)),
        afterBody = Nil
      )
    }
  }
}
