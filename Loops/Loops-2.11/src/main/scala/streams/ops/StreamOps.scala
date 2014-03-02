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
      case q"${StreamOp(src, ops)}.foreach[$_]((..$params) => $body)" =>
        (src, ops :+ ForeachOp(params, body))

      case q"${StreamOp(src, ops)}.map[$_, $_](${f @ Function(_, _)})($canBuildFrom)" =>
        (src, ops :+ MapOp(f, canBuildFrom))

      case q"${StreamOp(src, ops)}.filter($param => $body)" =>
        (src, ops :+ FilterOp(param, body))

      case q"${StreamOp(src, ops)}.withFilter($param => $body)" =>
        (src, ops :+ FilterOp(param, body))

      case StreamSource(src) =>
        (src, Nil)

      case ArrayOps(StreamSource(src)) =>
        (src, Nil)
    }
  }
}
