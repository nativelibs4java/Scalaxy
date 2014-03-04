package scalaxy.loops

private[loops] trait ArrayStreamSources extends Streams {
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

  object SomeArrayOps {
    def unapply(tree: Tree): Option[Tree] = Option(tree) collect {
      case q"scala.this.Predef.${AnyValArrayOpsName()}($a)" =>
        // println("ARRAY ANYVAL: " + a)
        a

      case q"scala.this.Predef.refArrayOps[$_]($a)" =>
        // println("ARRAY REF: " + a)
        a
    }
  }

  object SomeArrayStreamSource {
    def unapply(tree: Tree): Option[ArrayStreamSource] =
      SomeArrayOps.unapply(tree).map(ArrayStreamSource.apply _)
  }

  case class ArrayStreamSource(array: Tree)
      extends StreamSource
  {
    override def sinkOption = {
      println("TODO array sink")
      None
    }

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

      val (extractionCode, outputVars) = createTuploidPathsExtractionDecls(itemVal, outputNeeds, fresh)

      val StreamOpResult(streamPrelude, streamBody, streamEnding) =
        emitSub(outputVars, opsAndOutputNeeds, fresh, transform)

      q"""
        val $arrayVal = ${transform(array)}
        val $lengthVal = $arrayVal.length
        var $iVar = 0

        ..$streamPrelude
        while ($iVar < $lengthVal) {
          val $itemVal = $arrayVal($iVar)
          ..$streamBody
          $iVar += 1
        }
        ..$streamEnding
      """
    }
  }
}
