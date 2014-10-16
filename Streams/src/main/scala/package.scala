package scalaxy

import scala.language.reflectiveCalls

import scala.language.experimental.macros
import scala.language.implicitConversions

import scala.reflect.NameTransformer
import scala.reflect.macros.whitebox.Context

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

    private[this] def cast[A](a: Any): A = a.asInstanceOf[A]

    private[streams] def optimize[A : c.WeakTypeTag](c: Context)(a: c.Expr[A], recurse: Boolean): c.Expr[A] = {
      if (disabled) {
        a
      } else {
        object Optimize extends StreamTransforms {
          override val global = c.universe
          import global._

          val result = try {

            val strategy = scalaxy.streams.optimization.aggressive

            c.internal.typingTransform(cast(a.tree))((tree_, api) => {
              val tree: Tree = cast(tree_)

              def apiDefault(tree: Tree): Tree = cast(api.default(cast(tree)))
              def apiRecur(tree: Tree): Tree = cast(api.recur(cast(tree)))
              def apiTypecheck(tree: Tree): Tree = cast(api.typecheck(cast(tree)))

              val result = tree match {
                case tree @ SomeStream(stream) if stream.isWorthOptimizing(strategy) =>
                  c.info(
                    cast(tree.pos),
                    Optimizations.optimizedStreamMessage(stream.describe()),
                    force = impl.verbose)

                  def untyped(tree: Tree) = ???

                  try {
                    stream
                      .emitStream(
                        n => TermName(c.freshName(n)),
                        apiRecur(_),
                        apiTypecheck(_),
                        untyped)
                      .compose(apiTypecheck(_))
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

        c.Expr[A](Optimize.result)
      }
    }
//     private[streams] def optimize[A : c.WeakTypeTag](c: Context)(a: c.Expr[A], recurse: Boolean): c.Expr[A] = {
//       if (disabled) {
//         a
//       } else {
//         try {
//           import c.universe._
//           def typed(tree: Tree) = {
//             //new RuntimeException("TYPING: " + tree).printStackTrace()
//             //tree
//             c.typecheck(tree.asInstanceOf[c.Tree]).asInstanceOf[Tree]
//           }
//           def untyped(tree: Tree) = c.untypecheck(tree.asInstanceOf[c.Tree]).asInstanceOf[Tree]
//           val opt =
//             Optimizations.optimize(c.universe)(
//               a.tree,
//               typed(_),
//               untyped(_),
//               c.freshName(_),
//               c.info(_, _, force = verbose),
//               c.error(_, _),
//               recurse,
//               Optimizations.matchStrategyTree(c.universe)(
//                 // EmptyTree))))
//                 c.inferImplicitValue(typeOf[OptimizationStrategy])
//               )
//             )


//           // Untypechecking is needed for some local symbols captured by inner lambdas that fail to find their "proxy"
//           // TODO: Investigate (bug can happen in safe mode with no side-effect detection in macro integration tests).
//           // c.untypecheck(
//           // println(showRaw(opt));
//           // val unt = c.untypecheck(opt)

//           // 
//           //def untt = c.internal.transform(opt)(null)
//           // val unt = c.internal.typingTransform(opt) {
//           //   //case (tree @ Ident(name: TermName), api)
//           //   case (tree, api)
//           //       if tree.symbol != null &&
//           //          // tree.symbol.isTerm &&
//           //          // tree.symbol.asTerm.isVal &&
//           //          tree.symbol.name.toString.matches(".*?\\$macro\\$.*") =>
//           //     // q"($name: ${tree.tpe})"
//           //     println("UNTYPING: " + tree + ": " + tree.symbol + " @ " + tree.symbol.owner)
//           //     c.untypecheck(tree)

//           //   case (tree, api) =>
//           //     tree
//           // }
//           // val unt = new Transformer {
//           //   override def transform(tree: Tree) = tree match {
//           //     case tree//Ident(name: TermName)
//           //         if tree.symbol != null &&
//           //            tree.symbol.isTerm &&
//           //            tree.symbol.asTerm.isVal &&
//           //            tree.symbol.name.toString.matches(".*?\\$macro\\$.*") =>
//           //            // tree.pos != NoPosition =>
//           //       println("UNTYPING: " + tree + ": " + tree.symbol + " @ " + tree.symbol.owner)
//           //       // c.typecheck(c.untypecheck(tree))
//           //       // q"($name: ${tree.tpe})"
//           //       c.untypecheck(tree)

//           //     case _ =>
//           //       super.transform(tree)
//           //   }
//           // } transform opt
//           val unt = c.internal.typingTransform(opt) {
//              //case (tree @ Ident(name: TermName), api)
//              case (tree, api)
//                  if tree.symbol != null &&
//                     // tree.symbol.isTerm &&
//                     // tree.symbol.asTerm.isVal &&
//                     tree.isInstanceOf[RefTree] &&
//                     tree.symbol.name.toString.matches(".*?\\$macro\\$.*") =>
// //               println("UNTYPING: " + tree + ": " + tree.symbol + " @ " + tree.symbol.owner)
//                var tpe = tree.symbol.typeSignature.normalize
//                val ttree = c.untypecheck(tree)
// //               println("\tRETYPING: " + tpe + ": " + tpe.typeSymbol)
//                HacksAndWorkarounds.call(ttree, "setType", tpe)

//                ttree

// //             case (tree, api) if tree.symbol != null && tree.isInstanceOf[DefTree] =>
// //               println(s"currentOwner = ${api.currentOwner} ; currentOwner.info.decls = ${api.currentOwner.info.decls}")
// //               //api.currentOwner.info.decls enter tree.symbol
// //               HacksAndWorkarounds.call(api.currentOwner.info.decls, "enter", tree.symbol)
// //               api.default(tree)

//              case (tree, api) =>
//                api.default(tree)
//           }
// //          val unt0 = new Transformer {
// //            override def transform(tree: Tree) = tree match {
// //              case tree//
// //                //Ident(name: TermName)
// //                  if tree.symbol != null &&
// //                     tree.symbol.isTerm &&
// //                     tree.symbol.asTerm.isVal &&
// //                     tree.isInstanceOf[RefTree] &&
// //                     tree.symbol.name.toString.matches(".*?\\$macro\\$.*") =>
// //                     // tree.pos != NoPosition =>
// //                println("UNTYPING: " + tree + ": " + tree.symbol + " @ " + tree.symbol.owner)
// //                //var tpe = tree.tpe.normalize
// //                var tpe = tree.symbol.typeSignature.normalize
// //                // c.typecheck(c.untypecheck(tree))
// //                // q"($name: ${tree.tpe})"
// //                val ttree = c.untypecheck(tree)
// //                HacksAndWorkarounds.call(ttree, "setType", tpe)
// //                println("\tRETYPING: " + tpe + ": " + tpe.typeSymbol)
// //
// //                ttree
// //
// //              case _ =>
// //                super.transform(tree)
// //            }
// //          } transform opt

//           c.Expr[A](
//             // opt
//             // ret
//             // c.typecheck(unt)
//             unt
//             // new Transformer {
//             //   override def transform(tree: Tree) = {
//             //     val t = super.transform(tree)
//             //     try { c.typecheck(t) } catch { case ex: Throwable => t }
//             //   }
//             // } transform unt
//             // unt
//           )
//         } catch {
//           case ex: Throwable =>
//             ex.printStackTrace()
//             a
//         }
//       }
//     }
  }
}
