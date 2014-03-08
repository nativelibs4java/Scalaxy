package scalaxy

import scala.language.experimental.macros
import scala.language.implicitConversions

import scala.reflect.NameTransformer
import scala.reflect.macros.Context

import scala.reflect.runtime.{ universe => ru }

package object loops {
  def optimize[A](a: A): A = macro impl.optimize[A]
}

/**
  Stream output:
    indexName: Option[TermName],
    sinkSizeName: Option[TermName],
    tupleNames: TupleNames

  Then:
    tuple match + tuple apply + tupleNames => replacedBody + valdefs + newTupleNames

*/
package loops {
  object impl {
    def optimize[A : c.WeakTypeTag](c: Context)(a: c.Expr[A]): c.Expr[A] = {
      try {
        object Optimize extends StreamOps {
          override val global = c.universe
          import global._

          override def typed(tree: Tree, tpe: Type) =
            c.typeCheck(tree.asInstanceOf[c.Tree], tpe.asInstanceOf[c.Type]).asInstanceOf[Tree]

          val original = a.tree.asInstanceOf[Tree]//c.typeCheck(a.tree)

          val result = new Transformer {
            override def transform(tree: Tree) = tree match {
              case SomeStream(stream) =>
                c.info(a.tree.pos, s"stream = $stream", force = true)
                stream.emitStream(n => c.fresh(n): TermName, transform(_))

              case _ =>
                super.transform(tree)
            }
          } transform original

          // println(s"""
          //   Result:
          //     $result
          // """)
        }

        c.Expr[A](c.typeCheck(Optimize.result.asInstanceOf[c.universe.Tree]))
      } catch {
        case ex: Throwable =>
          ex.printStackTrace()
          a
      }
    }
  }
}
