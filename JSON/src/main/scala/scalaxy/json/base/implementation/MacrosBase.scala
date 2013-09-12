package scalaxy.json.base

import scala.language.dynamics
import scala.language.experimental.macros
import scala.reflect.macros.Context

trait MacrosBase {

  private[json] def reifyByteArray(c: Context)(v: Array[Byte]): c.Expr[Array[Byte]] = {
    import c.universe._

    val Apply(Apply(TypeApply(target, tparams), _), impls) = reify(Array[Byte](0: Byte)).tree
    c.Expr[Array[Byte]](
      Apply(Apply(TypeApply(target, tparams), v.toList.map(c.literal(_).tree)), impls)
    )
  }
}
