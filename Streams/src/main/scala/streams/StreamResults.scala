package scalaxy.streams

private[streams] trait StreamResults extends TuploidValues {
  val global: scala.reflect.api.Universe
  import global._

  type OutputNeeds = Set[TuploidPath]

  case class StreamOutput(
      prelude: List[Tree] = Nil,
      beforeBody: List[Tree] = Nil,
      body: List[Tree] = Nil,
      afterBody: List[Tree] = Nil,
      ending: List[Tree] = Nil)
  {
    def flatten: List[Tree] =
      prelude ++ beforeBody ++ body ++ afterBody ++ ending

    def compose(typed: Tree => Tree) =
      typed(q"..$flatten")

    def map(f: Tree => Tree): StreamOutput =
      copy(
        prelude = prelude.map(f),
        beforeBody = beforeBody.map(f),
        body = body.map(f),
        afterBody = afterBody.map(f),
        ending = ending.map(f))
  }

  val NoStreamOutput = StreamOutput()

  case class StreamInput(
    vars: TuploidValue[Tree],
    outputSize: Option[Tree] = None,
    index: Option[Tree] = None,
    loopInterruptor: Option[Tree] = None,
    fresh: String => TermName,
    transform: Tree => Tree,
    currentOwner: Symbol,
    typed: Tree => Tree)
}
