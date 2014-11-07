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
    with TakeDropOps
    with TakeWhileOps
    with ZipWithIndexOps
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeStreamOps extends Extractor[Tree, (Tree, List[StreamOp])] {
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
      SomeTakeDropOp,
      SomeTakeWhileOp,
      SomeToCollectionOp,
      SomeZipWithIndexOp
    )

    object ExtractOps {
      def unapply(extractorAndTree: (StreamOpExtractor, Tree)): Option[(Tree, List[StreamOp])] = {
        val (extractor, tree) = extractorAndTree
        extractor.unapply(tree) collect {
          case (SomeStreamOps(src, ops), op)
              if !ops.lastOption.exists(_.sinkOption == Some(ScalarSink)) =>
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
    //   case SomeForeachOp(SomeStreamOps(src, ops), op) =>
    //     (src, ops :+ op)

    //   case SomeCoerceOp(SomeStreamOps(src, ops), op) =>
    //     (src, ops :+ op)

    //   case SomeMapOp(SomeStreamOps(src, ops), op) =>
    //     (src, ops :+ op)

    //   case SomeCountOp(SomeStreamOps(src, ops), op) =>
    //     (src, ops :+ op)

    //   case SomeCollectOp(SomeStreamOps(src, ops), op) =>
    //     (src, ops :+ op)

    //   case SomeFlatMapOp(SomeStreamOps(src, ops), op) =>
    //     (src, ops :+ op)

    //   case SomeFindOp(SomeStreamOps(src, ops), op) =>
    //     (src, ops :+ op)

    //   case SomeWhileOp(SomeStreamOps(src, ops), op) =>
    //     (src, ops :+ op)

    //   case SomeOptionOp(SomeStreamOps(src, ops), op) =>
    //     (src, ops :+ op)

    //   case SomeFilterOp(SomeStreamOps(src, ops), op) =>
    //     (src, ops :+ op)

    //   case SomeZipWithIndexOp(SomeStreamOps(src, ops), op) =>
    //     (src, ops :+ op)

    //   case SomeArrayOpsOp(SomeStreamOps(src, ops), op) =>
    //     (src, ops :+ op)

    //   case SomeToCollectionOp(SomeStreamOps(src, ops), op) =>
    //     (src, ops :+ op)

    //   case _ =>
    //     (tree, Nil)
    // }
  }
}
