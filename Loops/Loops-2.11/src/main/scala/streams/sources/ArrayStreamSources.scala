package scalaxy.loops

private[loops] trait ArrayStreamSources extends Streams with ArrayBufferSinks {
  val global: scala.reflect.api.Universe
  import global._

  object AnyValArrayOpsName {
    def unapply(name: Name): Boolean = String.valueOf(name) match {
      case "intArrayOps" | "longArrayOps" | "byteArrayOps" | "shortArrayOps" |
        "charArrayOps" | "booleanArrayOps" | "floatArrayOps" | "doubleArrayOps" =>
        true

      case _ =>
        false
    }
  }

  lazy val ArraySym = rootMirror.staticClass("scala.Array")
  // Testing the type would be so much better, but yields an awkward MissingRequirementError.
  // lazy val ArrayTpe = typeOf[Array[_]]

  object SomeArrayOps extends Extractor[Tree, Tree]
  {
    def unapply(tree: Tree): Option[Tree] = Option(tree) collect {
      case q"scala.this.Predef.${AnyValArrayOpsName()}($a)" => a
      case q"scala.this.Predef.refArrayOps[$_]($a)"         => a
    }
  }

  object SomeArrayStreamSource extends Extractor[Tree, ArrayStreamSource] {
    def unapply(tree: Tree): Option[ArrayStreamSource] = Option(tree) collect {
      case _ if tree.tpe != null && tree.tpe != NoSymbol && tree.tpe.typeSymbol == ArraySym =>
        ArrayStreamSource(tree)
    }
  }

  case class ArrayStreamSource(array: Tree)
      extends StreamSource
  {
    override def sinkOption = Some(ArrayBufferSink)

    override def emitSource(
        outputNeeds: Set[TuploidPath],
        opsAndOutputNeeds: List[(StreamOp, Set[TuploidPath])],
        fresh: String => TermName,
        transform: Tree => Tree): Tree =
    {
      val arrayVal = fresh("array")
      val lengthVal = fresh("length")
      val iVar = fresh("i")
      val itemVal = fresh("item")

      // Early typing / symbolization.
      val Block(List(arrayValDef, lengthValDef, iVarDef, itemValDef, lengthValRef, iVarRef, itemValRef), _) = typed(q"""
        private[this] val $arrayVal = ${transform(array)}
        private[this] val $lengthVal = $arrayVal.length;
        private[this] var $iVar = 0;
        private[this] val $itemVal = $arrayVal($iVar);
        $lengthVal;
        $iVar;
        $itemVal
        {}
      """)
      val (extractionCode, outputVars) = createTuploidPathsExtractionDecls(itemValRef, outputNeeds, fresh)

      val StreamOpResult(streamPrelude, streamBody, streamEnding) =
        emitSub(outputVars, opsAndOutputNeeds, fresh, transform)

      // q"""
      //   private[this] val $arrayVal = ${transform(array)}
      //   private[this] val $lengthVal = $arrayVal.length
      //   private[this] var $iVar = 0

      //   ..$streamPrelude
      //   while ($iVar < $lengthVal) {
      //     val $itemVal = $arrayVal($iVar)
      //     ..$extractionCode
      //     ..$streamBody
      //     $iVar += 1
      //   }
      //   ..$streamEnding
      // """
      typed(q"""
        $arrayValDef
        $lengthValDef
        $iVarDef

        ..$streamPrelude
        while ($iVarRef < $lengthValRef) {
          $itemValDef
          ..$extractionCode
          ..$streamBody
          $iVarRef += 1
        }
        ..$streamEnding
      """)
    }
  }
}
