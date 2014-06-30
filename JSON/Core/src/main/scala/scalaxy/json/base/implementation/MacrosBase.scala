package scalaxy.json.base

import scala.language.dynamics
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

trait MacrosBase {
  private[json] def reifyByteArray(c: Context)(v: Array[Byte]): c.Expr[Array[Byte]] = {
    import c.universe._
    val Apply(Apply(TypeApply(target, tparams), _), impls) = reify(Array[Byte](0: Byte)).tree
    c.Expr[Array[Byte]](q"$target[..$tparams](..$v)")
  }
}
