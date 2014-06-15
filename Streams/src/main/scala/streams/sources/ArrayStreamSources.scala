package scalaxy.streams

private[streams] trait ArrayStreamSources
    extends ArrayBuilderSinks
    with ArrayOpsOps
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeArrayStreamSource {
    // Testing the type would be so much better, but yields an awkward MissingRequirementError.
    // lazy val ArrayTpe = typeOf[Array[_]]
    private[this] lazy val ArraySym = rootMirror.staticClass("scala.Array")

    def unapply(tree: Tree): Option[ArrayStreamSource] = Option(tree) collect {
      case _ if tree.tpe != null && tree.tpe != NoType && tree.tpe.typeSymbol == ArraySym =>
        ArrayStreamSource(tree)
    }
  }

  case class ArrayStreamSource(
      array: Tree,
      describe: Option[String] = Some("Array"),
      sinkOption: Option[StreamSink] = Some(ArrayBuilderSink))
    extends StreamSource
  {
    override def lambdaCount = 0

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input.{ fresh, transform, typed }

      val arrayVal = fresh("array")
      val lengthVal = fresh("length")
      val iVar = fresh("i")
      val itemVal = fresh("item")

      // Early typing / symbolization.
      val Block(List(
          arrayValDef,
          lengthValDef,
          iVarDef,
          itemValDef,
          lengthValRef,
          iVarRef,
          itemValRef), _) = typed(q"""
        private[this] val $arrayVal = ${transform(array)};
        private[this] val $lengthVal = $arrayVal.length;
        private[this] var $iVar = 0;
        private[this] val $itemVal = $arrayVal($iVar);
        $lengthVal;
        $iVar;
        $itemVal;
        ""
      """)
      val (extractionCode, outputVars) = createTuploidPathsExtractionDecls(itemValRef, outputNeeds, fresh, typed)

      val sub = emitSub(
        input.copy(
          vars = outputVars,
          outputSize = Some(lengthValRef)),
        nextOps)

      sub.copy(
        beforeBody = Nil,
        body = List(typed(q"""
          $arrayValDef;
          $lengthValDef;
          $iVarDef;
          ..${sub.beforeBody};
          while ($iVarRef < $lengthValRef) {
            $itemValDef;
            ..$extractionCode
            ..${sub.body};
            $iVarRef += 1
          }
        """))
      )
    }
  }
}
