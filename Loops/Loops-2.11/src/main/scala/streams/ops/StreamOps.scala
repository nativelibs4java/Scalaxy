package scalaxy.loops

private[loops] trait StreamOps
    extends StreamSources
    with ForeachOps
    with MapOps
    with FilterOps
{
  val global: scala.reflect.api.Universe
  import global._

  object StreamOp extends StreamOpExtractor {
    def unapply(tree: Tree): Option[(StreamSource, List[StreamOp])] = Option(tree) collect {
      case ForeachOp(StreamOp(src, ops), op) =>
        (src, ops :+ op)

      case MapOp(StreamOp(src, ops), op) =>
        (src, ops :+ op)

      case FilterOp(StreamOp(src, ops), op) =>
        (src, ops :+ op)

      case StreamSource(src) =>
        (src, Nil)

      case ArrayOps(StreamSource(src)) =>
        (src, Nil)
    }
  }
}
