package scalaxy.streams

object Streams
{
  def optimizedStreamMessage(streamDescription: String): String =
      "[Scalaxy] Optimized stream: " + streamDescription

  def optimize(u: scala.reflect.api.Universe)
              (tree: u.Tree,
                typeCheck: u.Tree => u.Tree,
                fresh: String => String,
                info: (u.Position, String) => Unit,
                recurse: Boolean = true): u.Tree =
  {
    object Optimize extends StreamTransforms {
      override val global = u
      import global._

      private[this] val typed = typeCheck.asInstanceOf[Tree => Tree]

      val result = new Transformer {
        override def transform(tree: Tree) = tree match {
          case SomeStream(stream) =>
            info(
              tree.pos.asInstanceOf[u.Position],
              optimizedStreamMessage(stream.describe()))
            val result =
              stream.emitStream(
                n => TermName(fresh(n)),
                if (recurse) transform(_) else tree => tree,
                typed)
              .compose(typed)
            // println(result)

            typed(result)

          case _ =>
            super.transform(tree)
        }
      } transform tree.asInstanceOf[Tree]//typed(tree)
    }

    typeCheck(Optimize.result.asInstanceOf[u.Tree])
  }
}

private[streams] trait Streams extends StreamComponents
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
                   typed: Tree => Tree,
                   sinkNeeds: Set[TuploidPath] = sink.outputNeeds): StreamOutput =
    {
      val sourceNeeds :: outputNeeds = ops.scanRight(sinkNeeds)({ case (op, refs) =>
        op.transmitOutputNeedsBackwards(refs)
      })
      val nextOps = ops.zip(outputNeeds) :+ (sink, sinkNeeds)
      // println(s"source = $source")
      // println(s"""ops =\n\t${ops.map(_.getClass.getName).mkString("\n\t")}""")
      // println(s"outputNeeds = ${nextOps.map(_._2)}")
      source.emit(
        input = StreamInput(
          vars = UnitTreeScalarValue,
          fresh = fresh,
          transform = transform,
          typed = typed),
        outputNeeds = sourceNeeds,
        nextOps = nextOps)
    }
  }
}
