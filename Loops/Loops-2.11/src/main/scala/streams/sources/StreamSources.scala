package scalaxy.loops

private[loops] trait StreamSources
  extends StreamComponents
  with InlineRangeStreamSources
  with ArrayStreamSources
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeStreamSource {
    def unapply(tree: Tree): Option[StreamSource] = Option(tree) collect {
      case SomeInlineRangeStreamSource(source) =>
        source

      case SomeArrayStreamSource(source) =>
        source
    }
  }
}
