package scalaxy.loops

private[loops] trait ForeachOps extends StreamSources {
  val global: scala.reflect.api.Universe
  import global._

  object ForeachOp {
    def unapply(tree: Tree): Option[(Tree, ForeachOp)] = Option(tree) collect {
      case q"$target.foreach[$_]((..$params) => $body)" =>
        (target, ForeachOp(params, body))
    }
  }
  case class ForeachOp(params: List[ValDef], body: Tree)
      extends StreamOp
      with StreamSink
  {
    override def emitOp(
        streamVars: StreamVars,
        ops: List[StreamOp],
        sink: StreamSink,
        fresh: String => TermName,
        transform: Tree => Tree): StreamOpResult =
    {
      require(ops.isEmpty)
      require(this == sink)
      StreamOpResult(
        prelude = Nil,
        // TODO match params and any tuple extraction in body with streamVars, replace symbols with streamVars values
        body = List(transform(body)),
        ending = Nil)
    }

    override def emitSink(
      streamVars: StreamVars,
      fresh: String => TermName,
      transform: Tree => Tree): StreamOpResult = ???
  }
}
