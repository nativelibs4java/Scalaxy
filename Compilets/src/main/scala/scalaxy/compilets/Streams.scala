package scalaxy; package compilets

object Streams extends Compilet {
  // TODO add conditions macro + isSideEffectFree(f)
  def mapMap[A, B, C](col: List[A], f: A => B, g: B => C) = replace(
    col.map(f).map(g),
    col.map(a => {
      //g(f(a))
      val b = f(a)
      val c = g(b)
      c
    })
  )
}
