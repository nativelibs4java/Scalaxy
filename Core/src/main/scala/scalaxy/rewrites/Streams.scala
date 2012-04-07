package scalaxy; package rewrites

import Macros._

object Streams {
  def mapMap[A, B, C](col: Seq[A], f: A => B, g: B => C) = Replacement(
    col.map(f).map(g),
    col.map(a => {
      val b = f(a)
      val c = g(b)
      c
    })
  )
}
