package scalaxy.streams

import impl.withQuietWarnings

private[streams] trait FlattenOps
    extends ClosureStreamOps
    with CanBuildFromSinks
    with Streams
    with Strippers
{
  val global: scala.reflect.api.Universe
  import global._

  val SomeStream: Extractor[Tree, Stream]

  object SomeFlattenOp extends StreamOpExtractor {
    private[this] lazy val PredefSymbol = rootMirror.staticModule("scala.Predef")

    private[this] def isAKnownAsTraversable(asTrav: Tree, targetTpe: Type) = asTrav match {
      case q"$predef.`$conforms`[$colTpt]" if predef.symbol == PredefSymbol =>
        targetTpe match {
          case TypeRef(_, _, List(internalColTpe)) =>
            internalColTpe =:= colTpt.tpe
          case _ =>
            false
        }

      case Strip(Function(List(param), Option2Iterable(ref))) if param.symbol == ref.symbol =>
        true

      case _ =>
        false
    }

    override def unapply(tree: Tree) = Option(tree) collect {
      case q"$target.flatten[$tpt]($asTrav)" if isAKnownAsTraversable(asTrav, target.tpe) =>
        (target, FlattenOp(tpt.tpe))
    }
  }

  case class FlattenOp(tpe: Type) extends StreamOp
  {
    override def describe = Some("flatten")

    override def lambdaCount = 0

    override def subTrees = Nil

    override def canInterruptLoop = false

    override def canAlterSize = true

    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) = paths

    override val sinkOption = None

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input.{ vars, fresh, transform, typed, currentOwner }

      val itemVal = fresh("item")
      val Function(List(itemValDef @ ValDef(_, _, _, _)), itemValRef @ Ident(_)) = typed(q"""
        ($itemVal: $tpe) => $itemVal
      """)

      val sub = emitSub(
        input.copy(
          vars = ScalarValue(tpe, alias = Some(itemValRef)),
          outputSize = None,
          index = None),
        nextOps)

      sub.copy(body = List(withQuietWarnings(transform(typed(q"""
        ${vars.alias.get}.foreach(($itemValDef) => {
          ..${sub.body};
        })
      """)))))
    }
  }
}
