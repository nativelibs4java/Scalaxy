
trait StreamSources
  extends Streams
  with InlineRangeStreamSources
  with ArrayStreamSources
{
  val global: scala.reflect.api.Universe
  import global._

  object StreamSource {
    def unapply(tree: Tree): Option[StreamSource] = Option(tree) collect {
      case InlineRangeStreamSource(source) =>
        source

      case ArrayStreamSource(source) =>
        source
    }
  }
}
