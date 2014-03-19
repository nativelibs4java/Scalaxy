package scalaxy.streams

private[streams] trait SeqStreamSources
    extends ArrayStreamSources
    with ListBufferSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeSeqStreamSource {
    private[this] lazy val SeqModuleSym = rootMirror.staticModule("scala.collection.Seq")
    private[this] lazy val ListModuleSym = rootMirror.staticModule("scala.collection.immutable.List")

    def unapply(tree: Tree): Option[StreamSource] = Option(tree) collect {
      case q"$seq.apply[$tpe](..$elements)" if seq.symbol == SeqModuleSym =>
        ArrayStreamSource(
          q"scala.Array.apply[$tpe](..$elements)",
          describe = Some("Seq"),
          sinkOption = Some(ListBufferSink))

      case q"$list.apply[$tpe](..$elements)" if list.symbol == ListModuleSym =>
        ArrayStreamSource(
          q"scala.List.apply[$tpe](..$elements)",
          describe = Some("List"),
          sinkOption = Some(ListBufferSink))
    }
  }
}
