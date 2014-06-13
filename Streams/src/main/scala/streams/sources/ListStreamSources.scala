package scalaxy.streams

private[streams] trait ListStreamSources
    extends ListBufferSinks
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

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input.{ fresh, transform, typed }

      val listVar = fresh("list")
      val itemVal = fresh("item")

      // Early typing / symbolization.
      val Block(List(
          listVarDef,
          itemValDef,
          nonEmptyListTest,
          listVarUpdate,
          itemValRef), _) = typed(q"""
        private[this] var $listVar = ${transform(list)};
        private[this] val $itemVal = $listVar.head;
        $listVar ne Nil;
        $listVar = $listVar.tail;
        $itemVal;
        ""
      """)
      val (extractionCode, outputVars) = createTuploidPathsExtractionDecls(itemValRef, outputNeeds, fresh, typed)

      val sub = emitSub(
        input.copy(
          vars = outputVars,
          outputSize = None),
        nextOps)
      sub.copy(body = List(typed(q"""
        $listVarDef;

        while ($nonEmptyListTest) {
          $itemValDef;
          ..$extractionCode
          ..${sub.body};
          $listVarUpdate
        }
      """)))
    }
  }
}
