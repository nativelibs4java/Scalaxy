package scalaxy.loops

private[loops] trait StreamResults extends TuploidValues {
  val global: scala.reflect.api.Universe
  import global._

  case class StreamOpOutput(
      prelude: List[Tree] = Nil,
      body: List[Tree] = Nil,
      ending: List[Tree] = Nil)
  {
    def compose = typed(q"..${prelude ++ body ++ ending}")
    def map(f: Tree => Tree): StreamOpOutput =
      copy(prelude = prelude.map(f), body = body.map(f), ending = ending.map(f))
  }

  val NoStreamOpOutput = StreamOpOutput(prelude = Nil, body = Nil, ending = Nil)

  case class StreamOpInput(
    values: TuploidValue[Tree],
    outputSize: Option[Tree],
    index: Option[Tree],
    fresh: String => TermName,
    transform: Tree => Tree)
}
