package scalaxy.loops

private[loops] trait Streams extends StreamSources with StreamOps
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeStream extends Extractor[Tree, Stream] {
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
}
