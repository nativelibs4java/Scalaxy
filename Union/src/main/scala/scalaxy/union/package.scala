package scalaxy

import scala.language.experimental.macros
import scala.reflect.macros.Context

package union {
  /**
   * Union type.
   */
  trait |[A, B]
}
package object union {
  implicit def conformsTo[A, B]: A =:= B = macro internal.conformsTo[A, B]

  object internal {
    def conformsTo[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context): c.Expr[A =:= B] = {
      import c.universe._

      def typeConformsTo(a: Type, b: Type): Boolean = {
        b == NoType || b =:= typeOf[Nothing] ||
          a =:= b ||
          b <:< typeOf[_ | _] && {
            val TypeRef(_, _, targs) = b
            targs != null && targs.exists(t => typeConformsTo(a, t))
          }
      }
      val a = weakTypeTag[A].tpe
      val b = weakTypeTag[B].tpe
      if (!typeConformsTo(a, b)) {
        c.error(c.prefix.tree.pos, "Type " + a + " does not conform to " + b)
      }
      if (a =:= b)
        reify(=:=.tpEquals[A].asInstanceOf[A =:= B])
      else
        c.literalNull
    }
  }
}
