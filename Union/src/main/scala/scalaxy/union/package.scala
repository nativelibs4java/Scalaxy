package scalaxy

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

package union {
  private[union] trait Trait
}

package object union {

  implicit class AnyOps[A](value: A) {
    def as[B]: B = macro scalaxy.union.internal.as[A, B]
  }

  def wrap[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context)(value: c.Expr[A]): c.Expr[B] = {
    import c.universe._

    scalaxy.union.internal.checkMemberOfUnion[A, B](c)

    val valueExpr = value
    val expr = reify {
      new Trait {
        val value: Any = valueExpr.splice
      }
    }

    val t = typeOf[Trait]
    val b = weakTypeTag[B].tpe // .dealias.widen
    var tree = expr.tree

    // Manual replace of base class Trait.
    // substituteSymbols would replace Trait with |, not |[A, B] (type params lost when calling tpe.typeSymbol).
    tree = (new Transformer() {
      override def transform(tree: Tree) = tree match {
        case Template(parents, self, body) =>
          Template(
            parents.map(t => {
              if (t.symbol == typeOf[Trait].typeSymbol)
                TypeTree(b)
              else
                transform(t)
            }),
            transform(self).asInstanceOf[ValDef],
            body.map(transform(_))
          )
        case _ =>
          super.transform(tree)
      }
    }).transform(tree)
    tree = internal.substituteSymbols(
      tree,
      List(typeOf[Trait].typeSymbol),
      List(b.typeSymbol)
    )
    c.Expr[B](tree)
  }
}
