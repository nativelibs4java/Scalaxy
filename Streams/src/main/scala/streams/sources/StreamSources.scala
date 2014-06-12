package scalaxy.streams

private[streams] trait StreamSources
  extends InlineRangeStreamSources
  with ListStreamSources
  with ArrayStreamSources
  with OptionStreamSources
  with InlineSeqStreamSources
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeStreamSource extends Extractor[Tree, StreamSource] {
    def unapply(tree: Tree): Option[StreamSource] = Option(tree) collect {
      case SomeInlineRangeStreamSource(source) => source
      case SomeInlineSeqStreamSource(source)   => source
      case SomeArrayStreamSource(source)       => source
      case SomeListStreamSource(source)        => source
      case SomeOptionStreamSource(source)      => source
    }
  }
}
