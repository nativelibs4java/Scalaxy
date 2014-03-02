package scalaxy.loops

private[loops] trait StreamOps
    extends StreamSources
    with ForeachOps
    with MapOps
    with FilterOps
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeStreamOp extends StreamOpExtractor {
    def unapply(tree: Tree): Option[(StreamSource, List[StreamOp])] = Option(tree) collect {
      case SomeForeachOp(SomeStreamOp(src, ops), op) =>
        (src, ops :+ op)

      case SomeMapOp(SomeStreamOp(src, ops), op) =>
        (src, ops :+ op)

      case SomeFilterOp(SomeStreamOp(src, ops), op) =>
        (src, ops :+ op)

      case SomeStreamSource(src) =>
        (src, Nil)

      case SomeArrayOps(SomeStreamSource(src)) =>
        (src, Nil)
    }
  }
}
