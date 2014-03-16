package scalaxy

import scala.language.experimental.macros
import scala.language.implicitConversions

import scala.reflect.NameTransformer
import scala.reflect.macros.Context

import scala.reflect.runtime.{ universe => ru }

package object streams {
  def optimize[A](a: A): A = macro impl.optimize[A]
}

package streams
{
  object impl
  {
    def optimizedStreamMessage(streamDescription: String): String =
      "[Scalaxy] Optimized stream: " + streamDescription

    def optimize[A : c.WeakTypeTag](c: Context)(a: c.Expr[A]): c.Expr[A] = {
      try {
        object Optimize extends StreamTransforms {
          override val global = c.universe
          import global._

          def typed(tree: Tree) = {
            c.typeCheck(
              tree.asInstanceOf[c.Tree])
              // tpe.asInstanceOf[c.Type])
            .asInstanceOf[Tree]
          }

          val original = a.tree.asInstanceOf[Tree]//c.typeCheck(a.tree)

          val result = new Transformer {
            override def transform(tree: Tree) = tree match {
              case SomeStream(stream) =>
                c.info(a.tree.pos, optimizedStreamMessage(stream.describe()), force = true)
                val result: Tree = stream.emitStream(n => c.fresh(n): TermName, transform(_), typed(_)).compose(typed(_))
                // println(tree)
                // println(result)

                //c.typeCheck(c.resetLocalAttrs(result.asInstanceOf[c.Tree]).asInstanceOf[c.Tree]).asInstanceOf[Tree]

                // result
                typed(tree)

              case _ =>
                super.transform(tree)
            }
          } transform original
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
