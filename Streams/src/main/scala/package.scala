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
    private[this] val verboseProperty = "scalaxy.streams.verbose"
    private[this] val veryVerboseProperty = "scalaxy.streams.veryVerbose"
    private[this] val optimizeProperty = "scalaxy.streams.optimize"

    def disabled: Boolean =
      System.getenv("SCALAXY_STREAMS_OPTIMIZE") == "0" ||
      System.getProperty(optimizeProperty) == "false"

    def verbose: Boolean =
      System.getenv("SCALAXY_STREAMS_VERBOSE") == "1" ||
      System.getProperty(verboseProperty) == "true"

    def verbose_=(v: Boolean) {
      System.setProperty(verboseProperty, v.toString)
    }

    def veryVerbose: Boolean =
      System.getenv("SCALAXY_STREAMS_VERY_VERBOSE") == "1" ||
      System.getProperty(veryVerboseProperty) == "true"

    def recursivelyOptimize[A : c.WeakTypeTag](c: Context)(a: c.Expr[A]): c.Expr[A] = {
      optimize[A](c)(a, recurse = true)
    }

    def optimizeTopLevelStream[A : c.WeakTypeTag](c: Context)(a: c.Expr[A]): c.Expr[A] = {
      optimize[A](c)(a, recurse = false)
    }

    private[streams] def optimize[A : c.WeakTypeTag](c: Context)(a: c.Expr[A], recurse: Boolean): c.Expr[A] = {
      if (disabled) {
        a
      } else {
        try {
          import c.universe._
          def typed(tree: Tree) = c.typecheck(tree.asInstanceOf[c.Tree]).asInstanceOf[Tree]
          def untyped(tree: Tree) = c.untypecheck(tree.asInstanceOf[c.Tree]).asInstanceOf[Tree]
          c.Expr[A](
            // Untypechecking is needed for some local symbols captured by inner lambdas that fail to find their "proxy"
            // TODO: Investigate (bug can happen in safe mode with no side-effect detection in macro integration tests).
            c.untypecheck(
              Optimizations.optimize(c.universe)(
                a.tree,
                typed(_),
                untyped(_),
                c.freshName(_),
                c.info(_, _, force = verbose),
                c.error(_, _),
                recurse,
                Optimizations.matchStrategyTree(c.universe)(
                  // EmptyTree))))
                  c.inferImplicitValue(typeOf[OptimizationStrategy])
                )
              )
            )
          )
        } catch {
          case ex: Throwable =>
            ex.printStackTrace()
            a
        }
      }
    }
  }
}
