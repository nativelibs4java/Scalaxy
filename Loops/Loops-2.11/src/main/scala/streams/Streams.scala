package scalaxy.loops

private[loops] trait Streams {
  val global: scala.reflect.api.Universe
  import global._

  object TODO {
    sealed trait StreamValueType
    case class ScalarStreamValueType(tpe: Type) extends StreamValueType
    case class TupleStreamValueType(elements: ScalarStreamValueType) extends StreamValueType

    case class StreamFiber(path: List[Int])
    case class StreamVars(fiberNames: Map[StreamFiber, TermName] = Map()) {
      // zip, etc...
    }
  }
  case class StreamVars(
    valueName: TermName,
    originalName: TermName = null,
    originalSymbol: Symbol = NoSymbol)

  trait StreamComponent {
    def emitSub(streamVars: StreamVars,
                ops: List[StreamOp],
                sink: StreamSink,
                fresh: String => TermName,
                transform: Tree => Tree): StreamOpResult =
    {
      ops match {
        case firstOp :: otherOps =>
          firstOp.emitOp(streamVars, otherOps, sink, fresh, transform)

        case Nil =>
          sink.emitSink(streamVars, fresh, transform)
          // NoStreamOpResult
      }
    }

    def replaceClosureBody(streamVars: StreamVars, tree: Tree): Tree = {
      // TODO
      tree
    }
    def matchVars(streamVars: StreamVars, params: List[ValDef]): StreamVars = {
      // TODO
      streamVars
    }
  }

  trait StreamSource extends StreamComponent {
    def emitSource(ops: List[StreamOp],
                   sink: StreamSink,
                   fresh: String => TermName,
                   transform: Tree => Tree): Tree
  }

  trait StreamSink extends StreamComponent {
    def emitSink(streamVars: StreamVars,
                 fresh: String => TermName,
                 transform: Tree => Tree): StreamOpResult
  }

  case class StreamOpResult(prelude: List[Tree], body: List[Tree], ending: List[Tree])

  val NoStreamOpResult = StreamOpResult(prelude = Nil, body = Nil, ending = Nil)

  trait StreamOp extends StreamComponent {
    def emitOp(streamVars: StreamVars,
               ops: List[StreamOp],
               sink: StreamSink,
               fresh: String => TermName,
               transform: Tree => Tree): StreamOpResult
  }

  case class Stream(source: StreamSource, ops: List[StreamOp], sink: StreamSink) {
    def emitStream(fresh: String => TermName,
                   transform: Tree => Tree): Tree =
    {
      // val StreamOpResult(streamPrelude, streamBody, streamEnding) =
      //   source.emitSource(ops, sink, fresh, transform)

      // q"""
      //   ..$streamPrelude
      //   ..$streamBody
      //   ..$streamEnding
      // """

      source.emitSource(ops, sink, fresh, transform)
    }
  }

  trait StreamOpExtractor {
    def unapply(tree: Tree): Option[(StreamSource, List[StreamOp])]
  }

  val SomeStreamOp: StreamOpExtractor

  object Stream {
    def unapply(tree: Tree): Option[Stream] = tree match {
      case SomeStreamOp(source, ops @ (_ :: _)) =>
        (source :: ops).reverse collectFirst {
          case sink: StreamSink =>
            new Stream(source, ops, sink)
        }

      case _ =>
        None
    }
  }
}
