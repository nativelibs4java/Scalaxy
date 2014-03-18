package scalaxy.streams

private[streams] trait StreamSources
  extends InlineRangeStreamSources
  with ArrayStreamSources
  with OptionStreamSources
  with SeqStreamSources
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeStreamSource extends Extractor[Tree, StreamSource] {
    def unapply(tree: Tree): Option[StreamSource] = Option(tree) collect {
      case SomeInlineRangeStreamSource(source) =>
        source

      case SomeArrayStreamSource(source) =>
        source

      case SomeOptionStreamSource(source) =>
        source

      case SomeSeqStreamSource(source) =>
        source
    }
  }
}
