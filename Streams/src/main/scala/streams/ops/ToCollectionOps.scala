package scalaxy.streams

private[streams] trait ToCollectionOps
    extends StreamComponents
    with ListBufferSinks
    with ArrayBuilderSinks
    with VectorBuilderSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeToCollectionOp {
    def unapply(tree: Tree): Option[(Tree, ToCollectionOp)] = Option(tree) collect {
      case q"$target.toList" =>
        (target, ToListOp)

      case q"$target.toVector" =>
        (target, ToVectorOp)

      case q"$target.toArray[${_}](${_})" =>
        (target, ToArrayOp)
    }
  }

  class ToCollectionOp(name: String, sink: StreamSink) extends PassThroughStreamOp {
    override def describe = Some(name)
    override def sinkOption = Some(sink)
    override def lambdaCount = 0
    override def canAlterSize = false
  }

  case object ToListOp extends ToCollectionOp("toList", ListBufferSink)

  case object ToArrayOp extends ToCollectionOp("toArray", ArrayBuilderSink)

  case object ToVectorOp extends ToCollectionOp("toVector", VectorBuilderSink)
}
