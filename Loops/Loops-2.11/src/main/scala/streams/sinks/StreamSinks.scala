package scalaxy.loops

private[loops] trait StreamSinks extends StreamComponents with BuilderSinks {
  val global: scala.reflect.api.Universe
  import global._

  object SomeStreamSink extends Extractor[Tree, (Tree, StreamSink)] {
    def unapply(tree: Tree): Option[(Tree, StreamSink)] = Option(tree) collect {
      case q"$target.toList" =>
        (target, ListBufferSink)

      case q"$target.toArray[$_]($_)" =>
        (target, ArrayBuilderSink)

      case q"$target.toSet" =>
        (target, SetBuilderSink)
    }
  }
}
