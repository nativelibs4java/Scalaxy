package scalaxy

import scala.language.reflectiveCalls

import scala.language.experimental.macros
import scala.language.implicitConversions

import scala.reflect.NameTransformer
import scala.reflect.macros.blackbox.Context

import scala.reflect.runtime.{ universe => ru }

import scalaxy.streams.HacksAndWorkarounds.{cast, safelyUnSymbolize}

package object streams {
  def optimize[A](a: A): A = macro impl.recursivelyOptimize[A]
}

package streams
{
  object impl
  {
    private[streams] var debug: Boolean =
      System.getenv("SCALAXY_STREAMS_DEBUG") == "1" ||
      System.getProperty("scalaxy.streams.debug") == "true"

    private[streams] var veryVerbose: Boolean =
      debug ||
      System.getenv("SCALAXY_STREAMS_VERY_VERBOSE") == "1" ||
      System.getProperty("scalaxy.streams.veryVerbose") == "true"

    // TODO: optimize this (trait).
    private[streams] var verbose: Boolean =
      veryVerbose ||
      System.getenv("SCALAXY_STREAMS_VERBOSE") == "1" ||
      System.getProperty("scalaxy.streams.verbose") == "true"

    private[streams] var disabled: Boolean =
      System.getenv("SCALAXY_STREAMS_OPTIMIZE") == "0" ||
      System.getProperty("scalaxy.streams.optimize") == "false"

    /** For testing */
    private[streams] var quietWarnings = false

    def recursivelyOptimize[A : c.WeakTypeTag](c: Context)(a: c.Expr[A]): c.Expr[A] = {
      optimize[A](c)(a, recurse = true)
    }

    def optimizeTopLevelStream[A : c.WeakTypeTag](c: Context)(a: c.Expr[A]): c.Expr[A] = {
      optimize[A](c)(a, recurse = false)
    }

    private[streams] def optimize[A : c.WeakTypeTag](c: Context)(a: c.Expr[A], recurse: Boolean): c.Expr[A] = {

      val strategy = Optimizations.matchStrategyTree(c.universe)(
        c.mirror.staticClass(_),
        tpe => c.inferImplicitValue(tpe, pos = a.tree.pos))

      if (disabled) {
        a
      } else {
        object Optimize extends StreamTransforms {
          override val global = c.universe
          import global._

          override def info(pos: Position, msg: String, force: Boolean) {
            c.info(cast(pos), msg, force = force)
          }
          override def warning(pos: Position, msg: String) {
            c.warning(cast(pos), msg)
          }
          override def error(pos: Position, msg: String) {
            c.error(cast(pos), msg)
          }

          val result = try {

            c.internal.typingTransform(cast(a.tree))((tree_, api) => {
              val tree: Tree = cast(tree_)

              // TODO(ochafik): Remove these warts (needed because of dependent types mess).
              def apiDefault(tree: Tree): Tree = cast(api.default(cast(tree)))
              def apiRecur(tree: Tree): Tree =
                if (recurse)
                  cast(api.recur(cast(tree)))
                else
                  tree
              def apiTypecheck(tree: Tree): Tree = cast(api.typecheck(cast(tree)))

              val result = transformStream(
                tree = tree,
                strategy = strategy,
                fresh = c.freshName(_),
                currentOwner = cast(api.currentOwner),
                recur = apiRecur,
                typecheck = apiTypecheck) match {

                case Some(result) =>
                  result

                case _ if recurse =>
                  apiDefault(tree)

                case _ =>
                  tree
              }

              // println(s"result = $result")
              result.asInstanceOf[c.universe.Tree]
            })
          } catch {
            case ex: Throwable =>
              logException(cast(a.tree.pos), ex, warning)
              a.tree
          }
        }

        c.Expr[A](Optimize.result)
      }
    }
  }
}
