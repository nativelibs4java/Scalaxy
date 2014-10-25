package scalaxy.streams

private[streams] trait ListStreamSources
    extends ListBufferSinks
    with StreamInterruptors
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeListStreamSource {
    // Testing the type would be so much better, but yields an awkward MissingRequirementError.
    // lazy val ArrayTpe = typeOf[Array[_]]
    private[this] lazy val ListSym = rootMirror.staticClass("scala.List")

    def unapply(tree: Tree): Option[ListStreamSource] = Option(tree) collect {
      case _ if tree.tpe != null && tree.tpe != NoType && tree.tpe <:< typeOf[List[Any]] =>
        ListStreamSource(tree)
    }
  }

  case class ListStreamSource(
      list: Tree,
      describe: Option[String] = Some("List"),
      sinkOption: Option[StreamSink] = Some(ListBufferSink))
    extends StreamSource
  {
    override def lambdaCount = 0

    override def subTrees = List(list)

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input.{ fresh, transform, typed }

      val listVal = fresh("list")
      val listVar = fresh("currList")
      val itemVal = fresh("item")

      // Early typing / symbolization.
      val Block(List(
          listValDef,
          listVarDef,
          itemValDef,
          listVarUpdate),
          TupleCreation(List(
            listSize, nonEmptyListTest, itemValRef))) = typed(q"""
        private[this] val $listVal = ${transform(list)}
        private[this] var $listVar = $listVal;
        private[this] val $itemVal = $listVar.head;
        $listVar = $listVar.tail;
        ($listVal.size, $listVar ne Nil, $itemVal)
      """)
      val coercionSuccessVarDefRef =
        newCoercionSuccessVarDefRef(nextOps, fresh, typed)
      val (extractionCode, outputVars) =
        createTuploidPathsExtractionDecls(
          itemValRef, outputNeeds, fresh, typed,
          coercionSuccessVarDefRef)

      val interruptor = new StreamInterruptor(input, nextOps)

      val sub = emitSub(
        input.copy(
          vars = outputVars,
          loopInterruptor = interruptor.loopInterruptor,
          outputSize = Some(listSize)),
        nextOps,
        coercionSuccessVarDefRef._2)
      sub.copy(
        beforeBody = Nil,
        body = List(typed(q"""
          $listValDef;
          $listVarDef;
          ..${interruptor.defs}
          ..${sub.beforeBody};
          while (${interruptor.composeTest(nonEmptyListTest)}) {
            $itemValDef;
            ..$extractionCode
            ..${sub.body};
            $listVarUpdate
          }
        """))
      )
    }
  }
}
