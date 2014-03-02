package scalaxy.loops

private[loops] trait MapOps
    extends StreamSources
    with CanBuildFromSinks
    with TuploidValues
    with TransformationClosures
    with Strippers
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeMapOp {
    def unapply(tree: Tree): Option[(Tree, MapOp)] = Option(tree) collect {
      case q"$target.map[$_, $_](${Strip(f @ Function(_, _))})($canBuildFrom)" =>
        (target, MapOp(f, canBuildFrom))
    }
  }
  //params: List[ValDef], body: Tree, 
  case class MapOp(f: Function, canBuildFrom: Tree)
      extends StreamOp
      with CanBuildFromSink {
    override def emitOp(
        streamVars: StreamVars,
        ops: List[StreamOp],
        sink: StreamSink,
        fresh: String => TermName,
        transform: Tree => Tree): StreamOpResult =
    {
      for (SomeTransformationClosure(TransformationClosure(inputs, valDefs, outputs)) <- f) {
        println(s"""Rewiring Map:
          inputs = $inputs,
          valDefs = $valDefs,
          output = $outputs
        """)
      }

      val q"(..$params) => $body" = f
      //params: List[ValDef], body: Tree, 

      // TODO wire input and output fiber vars 
      val mapped = fresh("mapped")
      // TODO inject mapped in vars
      val subVars = matchVars(streamVars, params)
      val StreamOpResult(streamPrelude, streamBody, streamEnding) =
        emitSub(subVars, ops, sink, fresh, transform)

      val newBody = replaceClosureBody(subVars, transform(body))

      val builder = fresh("builder")
      StreamOpResult(
        // TODO pass source collection to canBuildFrom if it exists.
        prelude = streamPrelude,
        // TODO match params and any tuple extraction in body with streamVars, replace symbols with streamVars values
        body = List(q"""
          val $mapped = $newBody
          ..$streamBody
        """),
        ending = streamEnding
      )
    }
  }
}
