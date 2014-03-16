package scalaxy.streams

private[streams] trait StreamOps
    extends ForeachOps
    with MapOps
    with FlatMapOps // TODO
    with FilterOps
    with ZipWithIndexOps
    with ArrayOps
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeStreamOp extends Extractor[Tree, (Tree, List[StreamOp])]  {
    def unapply(tree: Tree): Option[(Tree, List[StreamOp])] = Option(tree) collect {
      case SomeForeachOp(SomeStreamOp(src, ops), op) =>
        (src, ops :+ op)

      case SomeMapOp(SomeStreamOp(src, ops), op) =>
        (src, ops :+ op)

      case SomeFlatMapOp(SomeStreamOp(src, ops), op) =>
        (src, ops :+ op)

      case SomeFilterOp(SomeStreamOp(src, ops), op) =>
        (src, ops :+ op)

      case SomeZipWithIndexOp(SomeStreamOp(src, ops), op) =>
        (src, ops :+ op)

      case SomeArrayOps(SomeStreamOp(src, ops)) =>
        (src, ops)

      case _ =>
        (tree, Nil)
    }
  }
}
