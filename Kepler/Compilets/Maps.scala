package scalaxy; package compilets

import macros._

object Maps {
  /*
  def arrayMap[A : TypeTag, B : TypeTag](a: Array[A], body: B) = replace(
    a.map(i => body),
    {
      val out = scala.collection.mutable.ArrayBuilder.make[B]
      var i = 0
      val n = a.length
      while (i < n) {
        out += body
        i += 1
      }
      out.result()
    }
  )
  */
}
