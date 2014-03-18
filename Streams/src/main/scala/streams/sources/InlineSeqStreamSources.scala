package scalaxy.streams

private[streams] trait SeqStreamSources
    extends ArrayStreamSources
    with ListBufferSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeSeqStreamSource {
    // Testing the type would be so much better, but yields an awkward MissingRequirementError.
    // lazy val ArrayTpe = typeOf[Array[_]]
    //private[this] lazy val SeqSym = rootMirror.staticClass("scala.collection.Seq")
    private[this] lazy val SeqModuleSym = rootMirror.staticModule("scala.collection.Seq")

    def unapply(tree: Tree): Option[StreamSource] = Option(tree) collect {
      case q"$seq.apply[$tpe](..$elements)" if seq.symbol == SeqModuleSym =>
          // if tree.tpe != null && tree.tpe != NoType && tree.tpe.typeSymbol == SeqSym =>
        ArrayStreamSource(
          q"scala.Array.apply[$tpe](..$elements)",
          describe = Some("Seq"),
          sinkOption = Some(ListBufferSink))
    }
  }
}
