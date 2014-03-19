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
        body match {
          case q"""
            ${Strip(Ident(name))} match {
              case ${CaseTuploidValue(inputValue, Literal(Constant(true)))}
              case _ => false
          }""" if name == param.name && body.tpe =:= typeOf[Boolean] =>
          Some(target, CoerceOp(inputValue))

        case _ =>
          None
        }

      case _ =>
        None
    }
  }

  case class CoerceOp(inputValue: TuploidValue[Symbol]) extends StreamOp {
    override def describe = Some("<coerce>") //None
    override def sinkOption = None

    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) =
      paths + RootTuploidPath

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input.typed

      val pathsThatNeedToBeNullChecked = inputValue.collect({
        case (path @ (_ :: _), _) =>
          path.dropRight(1)
      }).distinct


      val sub = emitSub(input, nextOps)
      if (pathsThatNeedToBeNullChecked.isEmpty) {
        sub
      } else {
        val conditions = pathsThatNeedToBeNullChecked.map(path => {
          // Expression that points to a tuple, e.g. `input._1._2`
          val expr = path.foldLeft(input.vars.alias.get.duplicate) {
            case (tree, i) =>
              Select(tree, ("_" + (i + 1)): TermName)
          }
          q"$expr != null"
        })
        val condition = conditions.reduceLeft((a, b) => q"a && b")

        sub.copy(body = List(typed(q"""
          if ($condition) {
            ..${sub.body};
          }
        """)))
      }
    }

  }
}
