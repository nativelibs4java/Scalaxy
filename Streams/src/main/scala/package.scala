package scalaxy

import scala.language.experimental.macros
import scala.language.implicitConversions

import scala.reflect.NameTransformer
import scala.reflect.macros.blackbox.Context

import scala.reflect.runtime.{ universe => ru }

package object streams {
  def optimize[A](a: A): A = macro impl.recursivelyOptimize[A]
}

package streams
{
  object impl
  {
    def disabled: Boolean =
      System.getenv("SCALAXY_STREAMS_OPTIMIZE") == "0" ||
      System.getProperty("scalaxy.streams.optimize") == "false"

    def recursivelyOptimize[A : c.WeakTypeTag](c: Context)(a: c.Expr[A]): c.Expr[A] = {
      optimize[A](c)(a, recurse = true)
    }

    def optimize[A : c.WeakTypeTag](c: Context)(a: c.Expr[A], recurse: Boolean): c.Expr[A] = {
      if (disabled) {
        a
      } else {
        try {
          import c.universe._
          def typed(tree: Tree) = c.typecheck(tree.asInstanceOf[c.Tree]).asInstanceOf[Tree]
          c.Expr[A](
            c.resetLocalAttrs(
              Streams.optimize(c.universe)(
                a.tree,
                typed(_),
                c.fresh(_),
                c.info(_, _, force = true), recurse)))
        } catch {
          case ex: Throwable =>
            ex.printStackTrace()
            a
        }
      }
    }
  }
}
