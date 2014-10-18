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

      val strategy = Optimizations.matchStrategyTree(c.universe)(
        c.mirror.staticClass(_),
        tpe => c.inferImplicitValue(tpe, pos = a.tree.pos))

      // println(s"strategy = $strategy")

      if (disabled) {
        a
      } else {
        object Optimize extends StreamTransforms {
          override val global = c.universe
          import global._

          val result = try {

            c.internal.typingTransform(cast(a.tree))((tree_, api) => {
              val tree: Tree = cast(tree_)

              // TODO(ochafik): Remove these warts (needed because of dependent types mess).
              def apiDefault(tree: Tree): Tree = cast(api.default(cast(tree)))
              def apiRecur(tree: Tree): Tree = cast(api.recur(cast(tree)))
              def apiTypecheck(tree: Tree): Tree = cast(api.typecheck(cast(tree)))

              val result = tree match {
                case tree @ SomeStream(stream) if stream.isWorthOptimizing(strategy) =>
                  c.info(
                    cast(tree.pos),
                    Optimizations.optimizedStreamMessage(stream.describe()),
                    force = impl.verbose)

                  def untyped(tree: Tree): Tree = cast(c.untypecheck(cast(tree)))

                  try {
                    val res = stream
                      .emitStream(
                        n => TermName(c.freshName(n)),
                        apiRecur(_),
//                        t => apiRecur(apiTypecheck(t)),//
                        apiTypecheck(_),
                        untyped)
                      .compose(apiTypecheck(_))

                    // println(s"RESULT = $res")
                    // safelyUnSymbolize(c)(cast(res))
                    res
                  } catch {
                    case ex: Throwable =>
                      ex.printStackTrace()
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
