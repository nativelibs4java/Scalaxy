package scalaxy.streams

private[streams] trait CoerceOps
  extends StreamComponents
  with Strippers
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeCoerceOp  extends StreamOpExtractor {
    override def unapply(tree: Tree) = tree match {
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

  case class CoerceOp(coercedInputValue: TuploidValue[Symbol]) extends StreamOp {
    override def describe = Some("withFilter") // Some("withFilter(checkIfRefutable)") //None
    override def lambdaCount = 1
    override def sinkOption = None
    override def canAlterSize = true
    override def subTrees = Nil

    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) =
      if (paths.isEmpty)
        Set(RootTuploidPath)
      else
        paths

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input.typed

      val sub = emitSub(input.copy(outputSize = None), nextOps)

      val pathsThatCantBeNull = input.vars.collect({
        case (path, t @ TupleValue(_, _, _, false)) =>
          path
      }).toSet

      val coercedTuplePaths = coercedInputValue.collect({
        case (path @ (_ :: _), _) =>
          path.dropRight(1)
      }).distinct

      val pathsThatNeedToBeNullChecked = coercedTuplePaths.filterNot(pathsThatCantBeNull)

      if (pathsThatNeedToBeNullChecked.isEmpty) {
        sub
      } else {
        val conditions = pathsThatNeedToBeNullChecked.map(path => {
          // Expression that points to a tuple, e.g. `input._1._2`
          val expr = path.foldLeft(input.vars.alias.get.duplicate) {
            case (tree, i) =>
              Select(tree, TermName("_" + (i + 1)))
          }
          q"$expr != null"
        })
        val condition = conditions.reduceLeft((a, b) => q"$a && $b")

        sub.copy(body = List(typed(q"""
          if ($condition) {
            ..${sub.body};
          }
        """)))
      }
    }

  }
}
