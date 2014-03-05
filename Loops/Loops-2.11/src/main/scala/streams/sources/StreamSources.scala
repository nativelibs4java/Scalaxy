package scalaxy.loops

private[loops] trait StreamSources extends Streams
{
  val global: scala.reflect.api.Universe
  import global._

  type InlineRangeStreamSource[_] <: StreamSource
  val SomeInlineRangeStreamSource: Extractor[Tree, InlineRangeStreamSource[_]]

  type ArrayStreamSource <: StreamSource
  val SomeArrayStreamSource: Extractor[Tree, ArrayStreamSource]
  val SomeArrayOps: Extractor[Tree, Tree]

  object SomeStreamSource {
    def unapply(tree: Tree): Option[StreamSource] = Option(tree) collect {
      case SomeInlineRangeStreamSource(source) =>
        source

      case SomeArrayStreamSource(source) =>
        source
    }
  }
}
