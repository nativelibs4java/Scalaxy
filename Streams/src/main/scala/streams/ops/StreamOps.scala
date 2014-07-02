package scalaxy.streams

private[streams] trait StreamOps
    extends ArrayOpsOps
    with CoerceOps
    with FilterOps
    with FlatMapOps
    with ForeachOps
    with MapOps
    with ZipWithIndexOps
    with ToCollectionOps
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeStreamOp extends Extractor[Tree, (Tree, List[StreamOp])]  {
    def unapply(tree: Tree): Option[(Tree, List[StreamOp])] = Option(tree) collect {
      case SomeForeachOp(SomeStreamOp(src, ops), op) =>
        (src, ops :+ op)

      case SomeCoerceOp(SomeStreamOp(src, ops), op) =>
        (src, ops :+ op)

      case SomeMapOp(SomeStreamOp(src, ops), op) =>
        (src, ops :+ op)

      case SomeFlatMapOp(SomeStreamOp(src, ops), op) =>
        (src, ops :+ op)

      case SomeFilterOp(SomeStreamOp(src, ops), op) =>
        (src, ops :+ op)

      case SomeZipWithIndexOp(SomeStreamOp(src, ops), op) =>
        (src, ops :+ op)

      case SomeArrayOpsOp(SomeStreamOp(src, ops), op) =>
        (src, ops :+ op)

      case SomeToCollectionOp(SomeStreamOp(src, ops), op) =>
        (src, ops :+ op)

      case _ =>
        (tree, Nil)
    }
  }
}
