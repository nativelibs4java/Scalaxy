package scalaxy.loops

private[loops] trait Streams extends StreamComponents
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeStream extends Extractor[Tree, Stream] {
    def unapply(tree: Tree): Option[Stream] = tree match {
      case SomeStreamSink(SomeStreamOp(SomeStreamSource(source), ops @ (_ :: _)), sink) =>
        Some(new Stream(source, ops, sink))

      case SomeStreamOp(SomeStreamSource(source), ops @ (_ :: _)) =>
        (source :: ops).reverse.toIterator.map(_.sinkOption) collectFirst {
          case Some(sink) =>
            new Stream(source, ops, sink)
        }

      case _ =>
        None
    }
  }

  case class Stream(source: StreamSource, ops: List[StreamOp], sink: StreamSink)
  {
    def describe(describeSink: Boolean = true) =
      (source :: ops).flatMap(_.describe).mkString(".") +
      sink.describe.filter(_ => describeSink).map(" -> " + _).getOrElse("")

    def emitStream(fresh: String => TermName,
                   transform: Tree => Tree,
                   sinkNeeds: Set[TuploidPath] = sink.outputNeeds): StreamOpResult =
    {
      val sourceNeeds :: outputNeeds = ops.scanRight(sinkNeeds)({ case (op, refs) =>
        op.transmitOutputNeedsBackwards(refs)
      })
      val opsAndOutputNeeds = ops.zip(outputNeeds) :+ (sink, sinkNeeds)
      // println(s"source = $source")
      // println(s"""ops =\n\t${ops.map(_.getClass.getName).mkString("\n\t")}""")
      // println(s"outputNeeds = ${opsAndOutputNeeds.map(_._2)}")
      source.emitSource(sourceNeeds, opsAndOutputNeeds, fresh, transform)
    }
  }
}
