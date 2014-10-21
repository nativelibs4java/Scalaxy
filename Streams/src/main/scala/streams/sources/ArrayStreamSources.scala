package scalaxy.streams

private[streams] trait ArrayStreamSources
    extends ArrayBuilderSinks
    with ArrayOpsOps
    with StreamInterruptors
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeArrayStreamSource {
    // Testing the type would be so much better, but yields an awkward MissingRequirementError.
    // lazy val ArrayTpe = typeOf[Array[_]]
    private[this] lazy val ArraySym = rootMirror.staticClass("scala.Array")

    private[this] def isArrayType(tpe: Type) =
      Option(tpe).map(_.dealias.etaExpand).filter(_ != NoType).exists(_.typeSymbol == ArraySym)

    def unapply(tree: Tree): Option[ArrayStreamSource] = Option(tree) collect {
      case _ if isArrayType(tree.tpe) =>
                // || tree.symbol != null && isArrayType(tree.symbol.typeSignature) =>
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
    override def subTrees = List(array)

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

      val interruptor = new StreamInterruptor(input, nextOps)

      val sub = emitSub(
        input.copy(
          vars = outputVars,
          loopInterruptor = interruptor.loopInterruptor,
          outputSize = Some(lengthValRef)),
        nextOps)

      sub.copy(
        beforeBody = Nil,
        body = List(typed(q"""
          $arrayValDef;
          $lengthValDef;
          $iVarDef;
          ..${interruptor.defs}
          ..${sub.beforeBody};
          while (${interruptor.composeTest(q"$iVarRef < $lengthValRef")}) {
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
