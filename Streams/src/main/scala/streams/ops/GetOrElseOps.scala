package scalaxy.streams

private[streams] trait GetOrElseOps
    extends OptionSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeGetOrElseOp {
    def unapply(tree: Tree): Option[(Tree, GetOrElseOp)] = Option(tree) collect {
      case q"$target.get" =>
        (target, GetOrElseOp("get", GetOrElseSink(q"""
          throw new NoSuchElementException("None.get")
        """)))

      case q"$target.getOrElse[${_}]($v)" =>
        (target, GetOrElseOp("getOrElse", GetOrElseSink(v)))
    }
  }

  case class GetOrElseOp(name: String, sink: StreamSink) extends PassThroughStreamOp
  {
    override def describe = Some(name)

    override val sinkOption = Some(sink)

    override def lambdaCount = 0
  }
}
