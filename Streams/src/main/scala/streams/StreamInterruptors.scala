package scalaxy.streams

private[streams] trait StreamInterruptors extends StreamComponents
{
  val global: scala.reflect.api.Universe
  import global._

  class StreamInterruptor(input: StreamInput, nextOps: OpsAndOutputNeeds)
  {
    import input.{ fresh, typed }

    private[this] val continue = fresh("continue")

    private[this] val Block(List(
        continueVarDef,
        continueVarRef), _) = typed(q"""
      private[this] var $continue = true;
      $continue;
      ""
    """)

    private[this] val needsContinue = nextOps.exists(_._1.canInterruptLoop)

    val loopInterruptor = if (needsContinue) Some(continueVarRef) else None

    val (defs, test) =
      if (needsContinue)
        (Seq(continueVarDef), continueVarRef)
      else
        (Seq(), q"true")
  }
}
