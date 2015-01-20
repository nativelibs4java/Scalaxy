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
    private[this] lazy val ArraySym = rootMirror.staticClass("scala.Array")

    private[this] def isArrayType(tree: Tree) =
      findType(tree).exists(_.typeSymbol == ArraySym)

    def unapply(tree: Tree): Option[ArrayStreamSource] =
      Option(tree).filter(isArrayType).map(ArrayStreamSource(_))
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

      val arrayTpe = findType(array).orNull
      // getOrElse {
      //   sys.error(s"Failed to find type of $array")
      // }

      // Early typing / symbolization.
      val Block(List(
          arrayValDef,
          lengthValDef,
          iVarDef,
          itemValDef),
          TupleCreation(List(
            lengthValRef, iVarRef, itemValRef))) = typed(q"""
        private[this] val $arrayVal: $arrayTpe = ${transform(array)};
        private[this] val $lengthVal = $arrayVal.length;
        private[this] var $iVar = 0;
        private[this] val $itemVal = $arrayVal($iVar);
        ($lengthVal, $iVar, $itemVal)
      """)

      val TuploidPathsExtractionDecls(extractionCode, outputVars, coercionSuccessVarDefRef) =
        createTuploidPathsExtractionDecls(
          itemValRef.tpe, itemValRef, outputNeeds, fresh, typed,
          newCoercionSuccessVarDefRef(nextOps, fresh, typed))

      val interruptor = new StreamInterruptor(input, nextOps)

      val sub = emitSub(
        input.copy(
          vars = outputVars,
          loopInterruptor = interruptor.loopInterruptor,
          outputSize = Some(lengthValRef)),
        nextOps,
        coercionSuccessVarDefRef._2)

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
          ..${sub.afterBody}
        """)),
        afterBody = Nil
      )
    }
  }
}
