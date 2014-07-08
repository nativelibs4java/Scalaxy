package scalaxy.streams

private[streams] trait OptionOps
    extends OptionSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeOptionOp {
    def unapply(tree: Tree): Option[(Tree, OptionOp)] = Option(tree) collect {
      case q"$target.get" =>
        (target, OptionOp("get", OptionGetOrElseSink(q"""
          throw new NoSuchElementException("None.get")
        """)))

      case q"$target.getOrElse[${_}]($v)" =>
        (target, OptionOp("getOrElse", OptionGetOrElseSink(v)))

      case q"$target.isEmpty" =>
        (target, OptionOp("isEmpty", OptionIsEmptySink))
    }
  }

  case class OptionOp(name: String, sink: StreamSink) extends PassThroughStreamOp
  {
    override def describe = Some(name)

    override val sinkOption = Some(sink)

    /// Since this output scalars, the size is brought down to 0.
    override def canAlterSize = true

    override def lambdaCount = 0
  }
}
