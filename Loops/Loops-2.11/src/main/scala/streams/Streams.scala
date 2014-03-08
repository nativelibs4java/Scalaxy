package scalaxy.loops

private[loops] trait Streams extends StreamSources with StreamOps
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeStream {
    def unapply(tree: Tree): Option[Stream] = tree match {
      case SomeStreamOp(SomeStreamSource(source), ops @ (_ :: _)) =>
        (source :: ops).reverse.toIterator.map(_.sinkOption) collectFirst {
          case Some(sink) =>
            new Stream(source, ops, sink)
        }

      case _ =>
        None
    }
  }

  case class Stream(source: StreamSource, ops: List[StreamOp], sink: StreamSink) {
    def emitStream(fresh: String => TermName,
                   transform: Tree => Tree): Tree =
    {
      val sinkNeeds = sink.outputNeeds
      val sourceNeeds :: outputNeeds = ops.scanRight(sinkNeeds)({ case (op, refs) =>
        op.transmitOutputNeedsBackwards(refs)
      })
      val opsAndOutputNeeds = ops.zip(outputNeeds) :+ ((SinkOp(sink), sinkNeeds))
      // println(s"source = $source")
      // println(s"""ops =\n\t${ops.map(_.getClass.getName).mkString("\n\t")}""")
      // println(s"outputNeeds = ${opsAndOutputNeeds.map(_._2)}")
      source.emitSource(sourceNeeds, opsAndOutputNeeds, fresh, transform)
    }
  }
}
