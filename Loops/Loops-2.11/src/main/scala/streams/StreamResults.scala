package scalaxy.loops

private[loops] trait StreamResults extends TuploidValues {
  val global: scala.reflect.api.Universe
  import global._

  case class StreamOpResult(
      prelude: List[Tree] = Nil,
      body: List[Tree] = Nil,
      ending: List[Tree] = Nil)
  {
    def compose = typed(q"..${prelude ++ body ++ ending}")
    def map(f: Tree => Tree): StreamOpResult =
      copy(prelude = prelude.map(f), body = body.map(f), ending = ending.map(f))
  }

  val NoStreamOpResult = StreamOpResult(prelude = Nil, body = Nil, ending = Nil)
}
