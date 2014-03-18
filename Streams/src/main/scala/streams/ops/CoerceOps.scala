package scalaxy.streams

private[streams] trait CoerceOps
  extends StreamComponents
  with ClosureStreamOps
  with Strippers
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeCoerceOp {
    def unapply(tree: Tree): Option[(Tree, StreamOp)] = tree match {
      case q"$target.withFilter(${Strip(Function(List(param), body))})" =>
        // TODO
        body match {
          case q"""
            ${Strip(Ident(name))} match {
              case $_ => true
              case _ => false
          }""" if name == param.name && body.tpe =:= typeOf[Boolean] =>
          Some(target, CoerceOp(param, body))

        case _ =>
          None
        }

      case _ =>
        None
    }
  }

  case class CoerceOp(param: ValDef, body: Tree) extends ClosureStreamOp {
    override def describe = None
    override def sinkOption = None

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      null
    }

  }
}
