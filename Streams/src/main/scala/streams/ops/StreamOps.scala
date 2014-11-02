package scalaxy.streams

private[streams] trait StreamOps
    extends ArrayOpsOps
    with CoerceOps
    with CollectOps
    with CountOps
    with FilterOps
    with FindOps
    with ExistsOps
    with FlatMapOps
    with ForeachOps
    with IsEmptyOps
    with MapOps
    with OptionOps
    with ReductionOps
    with ToCollectionOps
    with WhileOps
    with ZipWithIndexOps
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeStreamOp extends Extractor[Tree, (Tree, List[StreamOp])] {
    val extractors = List[StreamOpExtractor](
      SomeArrayOpsOp,
      SomeCoerceOp,
      // TODO: fix intractable typing issues with case classes:
      //   SomeCollectOp,
      SomeCountOp,
      SomeExistsOp,
      SomeFilterOp,
      SomeFindOp,
      SomeFlatMapOp,
      SomeForeachOp,
      SomeIsEmptyOp,
      SomeMapOp,
      SomeOptionOp,
      SomeReductionOp,
      SomeToCollectionOp,
      SomeWhileOp,
      SomeZipWithIndexOp
    )

    object ExtractOps {
      def unapply(extractorAndTree: (StreamOpExtractor, Tree)): Option[(Tree, List[StreamOp])] = {
        val (extractor, tree) = extractorAndTree
        extractor.unapply(tree) collect {
          case (SomeStreamOp(src, ops), op) =>
            (src, ops :+ op)

          case (src, op) =>
            (src, List(op))
        }
      }
    }

    def unapply(tree: Tree): Option[(Tree, List[StreamOp])] = {
      extractors.toIterator.map(x => (x, tree)).collectFirst({
        case ExtractOps(src, ops) =>
          (src, ops)
      })
    }

    // def unapply(tree: Tree): Option[(Tree, List[StreamOp])] = Option(tree) collect {
    //   case SomeForeachOp(SomeStreamOp(src, ops), op) =>
    //     (src, ops :+ op)

    //   case SomeCoerceOp(SomeStreamOp(src, ops), op) =>
    //     (src, ops :+ op)

    //   case SomeMapOp(SomeStreamOp(src, ops), op) =>
    //     (src, ops :+ op)

    //   case SomeCountOp(SomeStreamOp(src, ops), op) =>
    //     (src, ops :+ op)

    //   case SomeCollectOp(SomeStreamOp(src, ops), op) =>
    //     (src, ops :+ op)

    //   case SomeFlatMapOp(SomeStreamOp(src, ops), op) =>
    //     (src, ops :+ op)

    //   case SomeFindOp(SomeStreamOp(src, ops), op) =>
    //     (src, ops :+ op)

    //   case SomeWhileOp(SomeStreamOp(src, ops), op) =>
    //     (src, ops :+ op)

    //   case SomeOptionOp(SomeStreamOp(src, ops), op) =>
    //     (src, ops :+ op)

    //   case SomeFilterOp(SomeStreamOp(src, ops), op) =>
    //     (src, ops :+ op)

    //   case SomeZipWithIndexOp(SomeStreamOp(src, ops), op) =>
    //     (src, ops :+ op)

    //   case SomeArrayOpsOp(SomeStreamOp(src, ops), op) =>
    //     (src, ops :+ op)

    //   case SomeToCollectionOp(SomeStreamOp(src, ops), op) =>
    //     (src, ops :+ op)

    //   case _ =>
    //     (tree, Nil)
    // }
  }
}
