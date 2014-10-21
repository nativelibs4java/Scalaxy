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
    private[streams] var disabled: Boolean =
      System.getenv("SCALAXY_STREAMS_OPTIMIZE") == "0" ||
      System.getProperty("scalaxy.streams.optimize") == "false"

    // TODO: optimize this (trait).
    private[streams] var verbose: Boolean =
      veryVerbose ||
      System.getenv("SCALAXY_STREAMS_VERBOSE") == "1" ||
      System.getProperty("scalaxy.streams.verbose") == "true"

    private[streams] var debug: Boolean =
      System.getenv("SCALAXY_STREAMS_DEBUG") == "1" ||
      System.getProperty("scalaxy.streams.debug") == "true"

    private[streams] var veryVerbose: Boolean =
      debug ||
      System.getenv("SCALAXY_STREAMS_VERY_VERBOSE") == "1" ||
      System.getProperty("scalaxy.streams.veryVerbose") == "true"

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

      // if (impl.veryVerbose) {
      //   c.info(a.tree.pos, Optimizations.messageHeader + s"Strategy = $strategy", force = true)
      // }

      if (disabled) {
        a
      } else {
        object Optimize extends StreamTransforms {
          override val global = c.universe
          import global._

          val info = (pos: Position, msg: String) => c.info(cast(pos), cast(msg), force = impl.verbose)
          val warning = (pos: Position, msg: String) => c.warning(cast(pos), cast(msg))

          val result = try {

            c.internal.typingTransform(cast(a.tree))((tree_, api) => {
              val tree: Tree = cast(tree_)

              // TODO(ochafik): Remove these warts (needed because of dependent types mess).
              def apiDefault(tree: Tree): Tree = cast(api.default(cast(tree)))
              def apiRecur(tree: Tree): Tree = cast(api.recur(cast(tree)))
              def apiTypecheck(tree: Tree): Tree = cast(api.typecheck(cast(tree)))

              val result = tree match {
                case tree @ SomeStream(stream) =>
                  if (isWorthOptimizing(stream, strategy, info, warning)) {
                    // TODO: move this (+ equiv code in StreamsComponent) to isWorthOptimizing
                    c.info(
                      cast(tree.pos),
                      Optimizations.optimizedStreamMessage(stream.describe(), strategy),
                      force = impl.verbose)

                    def untyped(tree: Tree): Tree = cast(c.untypecheck(cast(tree)))

                    try {
                      val result = stream
                        .emitStream(
                          n => TermName(c.freshName(n)),
                          apiRecur(_),
  //                        t => apiRecur(apiTypecheck(t)),//
                          currentOwner = cast[Symbol](api.currentOwner),
                          typed = apiTypecheck(_),
                          untyped = untyped)
                        .compose(apiTypecheck(_))

                      if (impl.debug) {
                        c.info(
                          cast(tree.pos),
                          Optimizations.messageHeader + s"Result for ${stream.describe()}:\n$result",
                          force = impl.verbose)
                      }
                      // safelyUnSymbolize(c)(cast(result))
                      result

                    } catch {
                      case ex: Throwable =>
                        ex.printStackTrace()
                        apiDefault(tree)
                    }
                  } else {
                    if (impl.veryVerbose) {
                      c.info(
                        cast(tree.pos),
                        Optimizations.messageHeader + s"Stream ${stream.describe()} is not worth optimizing with strategy $strategy",
                        force = impl.verbose)
                    }
                    apiDefault(tree)
                  }

                case tree if recurse =>
                  apiDefault(tree)

                case tree =>
                  assert(!recurse)
                  tree
              }

              // println(s"result = $result")
              result.asInstanceOf[c.universe.Tree]
            })
          } catch {
            case ex: Throwable =>
              ex.printStackTrace()
              a.tree
          }
        }

        //c.Expr[A](c.typecheck(c.untypecheck(Optimize.result)))
        c.Expr[A](Optimize.result)
      }
    }
  }
}
