package scalaxy.streams

private[streams] trait StreamSinks
    extends StreamComponents
    with ArrayBuilderSinks
    with ListBufferSinks
    with SetBuilderSinks
    with ReductionSinks
    with VectorBuilderSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeStreamSink extends Extractor[Tree, (Tree, StreamSink)] {
    def unapply(tree: Tree): Option[(Tree, StreamSink)] = Option(tree) collect {
      case q"$target.toList" =>
        (target, ListBufferSink)

      case q"$target.toArray[${_}](${_})" =>
        (target, ArrayBuilderSink)

      case q"$target.toSet[${_}]" =>
        (target, SetBuilderSink)

      case q"$target.toVector" =>
        (target, VectorBuilderSink)

      case q"$target.sum[${tpt}](${_})" =>
        (target, SumSink(tpt.tpe))

      case q"$target.product[${tpt}](${_})" =>
        (target, ProductSink(tpt.tpe))

      // case _ if { println(tree); false } => null
    }
  }
}
