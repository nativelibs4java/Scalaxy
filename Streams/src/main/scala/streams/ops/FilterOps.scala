package scalaxy.streams

private[streams] trait FilterOps
    extends ClosureStreamOps
    with Strippers
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeFilterOp extends StreamOpExtractor {
    override def unapply(tree: Tree)= Option(tree) collect {
      case q"$target.filter(${Closure(closure)})" =>
        (target, FilterOp(closure))

      case q"$target.filterNot(${Closure(closure)})" =>
        (target, FilterNotOp(closure))

      case q"$target.withFilter(${Closure(closure)})" =>
        (target, WithFilterOp(closure))
    }
  }

  case class FilterOp(override val closure: Function)
    extends FilterLikeOp(closure, isNegative = false, "filter")

  case class FilterNotOp(override val closure: Function)
    extends FilterLikeOp(closure, isNegative = true, "filterNot")

  case class WithFilterOp(override val closure: Function)
    extends FilterLikeOp(closure, isNegative = false, "withFilter")

  class FilterLikeOp(val closure: Function,
                     val isNegative: Boolean,
                     val name: String) extends ClosureStreamOp
  {
    override def describe = Some(name)

    override def sinkOption = None

    override def isMapLike = false

    override def canAlterSize = true

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input.typed

      val (replacedStatements, outputVars) =
        transformationClosure.replaceClosureBody(
          input,
          outputNeeds + RootTuploidPath)

      var test = outputVars.alias.get
      if (isNegative) {
        test = typed(q"!$test")
      }

      var sub = emitSub(input.copy(outputSize = None), nextOps)
      sub.copy(body = List(q"""
        ..$replacedStatements;
        if ($test) {
          ..${sub.body};
        }
      """))
    }
  }

}
