package scalaxy.loops

private[loops] trait FilterOps extends StreamSources with Blocks {
  val global: scala.reflect.api.Universe
  import global._

  object SomeFilterOp {
    def unapply(tree: Tree): Option[(Tree, FilterOp)] = Option(tree) collect {
      case q"$target.filter(${StripBlocks(Function(List(param), body))})" =>
        (target, FilterOp(param, body))

      case q"$target.withFilter(${StripBlocks(Function(List(param), body))})" =>
        (target, FilterOp(param, body))
    }
  }
  case class FilterOp(param: ValDef, body: Tree) extends StreamOp {
    override def emitOp(
        streamVars: StreamVars,
        ops: List[StreamOp],
        sink: StreamSink,
        fresh: String => TermName,
        transform: Tree => Tree): StreamOpResult =
    {
      val subVars = matchVars(streamVars, List(param))
      val StreamOpResult(streamPrelude, streamBody, streamEnding) =
        emitSub(subVars, ops, sink, fresh, transform)

      val test = replaceClosureBody(subVars, transform(body))

      StreamOpResult(
        prelude = streamPrelude,
        // TODO match params and any tuple extraction in body with streamVars, replace symbols with streamVars values
        body = List(
          q"""
            if ($test) {
              ..$streamBody
            }
          """
        ),
        ending = streamEnding
      )
    }
  }
}
