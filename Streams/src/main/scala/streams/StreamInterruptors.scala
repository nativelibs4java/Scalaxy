package scalaxy.streams

private[streams] trait StreamInterruptors
  extends StreamComponents
  with CoerceOps
{
  val global: scala.reflect.api.Universe
  import global._

  class StreamInterruptor(input: StreamInput, nextOps: OpsAndOutputNeeds)
  {
    import input.{ fresh, typed }

    private[this] val continue = fresh("continue")

    private[this] val Block(List(
        continueVarDef),
        continueVarRef) = typed(q"""
      private[this] var $continue = true;
      $continue
    """)

    private[this] val needsContinue = nextOps.exists(_._1.canInterruptLoop)

    val loopInterruptor: Option[Tree] = input.loopInterruptor orElse {
      if (needsContinue) Some(continueVarRef) else None
    }

    val (defs, test) =
      if (!input.loopInterruptor.isEmpty) {
        (Seq(), input.loopInterruptor.get)
      } else {
        if (needsContinue)
          (Seq(continueVarDef), continueVarRef)
        else
          (Seq(), q"true")
      }

    def composeTest(condition: Tree) = test match {
      case q"true" =>
        condition
      case _ =>
        q"$test && $condition"
    }
  }
}
