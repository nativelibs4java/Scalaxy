package scalaxy.streams

private[streams] trait ArrayStreamSources
    extends ArrayBuilderSinks
    with ArrayOps
{
  val global: scala.reflect.api.Universe
  import global._

  private[this] lazy val ArraySym = rootMirror.staticClass("scala.Array")
  // Testing the type would be so much better, but yields an awkward MissingRequirementError.
  // lazy val ArrayTpe = typeOf[Array[_]]

  object SomeArrayStreamSource {
    def unapply(tree: Tree): Option[ArrayStreamSource] = Option(tree) collect {
      case _ if tree.tpe != null && tree.tpe != NoSymbol && tree.tpe.typeSymbol == ArraySym =>
        ArrayStreamSource(tree)
    }
  }

  case class ArrayStreamSource(
      array: Tree,
      sinkOption: Option[StreamSink] = Some(ArrayBuilderSink))
    extends StreamSource
  {
    override def describe = Some("Array")

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
        {}
      """)
      val (extractionCode, outputVars) = createTuploidPathsExtractionDecls(itemValRef, outputNeeds, fresh, typed)

      val sub = emitSub(
        input.copy(
          vars = outputVars,
          outputSize = Some(lengthValRef)),
        nextOps)
      sub.copy(body = List(typed(q"""
        $arrayValDef;
        $lengthValDef;
        $iVarDef;

        while ($iVarRef < $lengthValRef) {
          $itemValDef;
          ..$extractionCode
          ..${sub.body};
          $iVarRef += 1
        }
      """)))
    }
  }
}
