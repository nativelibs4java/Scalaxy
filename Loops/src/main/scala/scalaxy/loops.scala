package scalaxy

import scala.language.experimental.macros
import scala.language.implicitConversions

import scala.reflect.macros.blackbox.Context

/** Scala loops compilation optimizations (special case of Scalaxy/Streams optimizations).
 *  Currently limited to Range foreach loops (no support for yield / map yet).
 *  Requires "inline" ranges (so the macro can extract start, end and optional step),
 *  and step must be a constant.
 *
 *  General syntax:
 *  <code>for (i <- start [to/until] end [by step]) { ... }</code>
 *
 *  Examples:
 *  <pre><code>
 *    import scalaxy.loops._
 *    val n = 1000000
 *    for (i <- 0 until n optimized) { ... }
 *    for (i <- n to 10 by -3 optimized) { ... }
 *  </code></pre>
 */
package object loops
{
  implicit def rangeExtensions(range: Range) =
    new RangeExtensions(range)

  private[loops] class RangeExtensions(range: Range)
  {
    /** Ensures a Range's foreach loop is compiled as an optimized while loop.
     *  Failure to optimize the loop will result in a compilation error.
     */
    def optimized: OptimizedRange = ???
  }

  private[loops] class OptimizedRange
  {
    /** Optimized foreach method.
     *  Only works if `range` is an inline Range with a constant step.
     */
    def foreach[U](f: Int => U): Unit =
      macro impl.rangeForeachImpl[U]

    def withFilter(f: Int => Boolean): OptimizedRange = ???

    def filter(f: Int => Boolean): OptimizedRange = ???

    /** This must not be executed at runtime
     *  (should be rewritten away by the foreach macro during compilation).
     */
    ???
  }
}

package loops
{
  package object impl
  {
    lazy val disabled =
      System.getenv("SCALAXY_LOOPS_OPTIMIZED") == "0" ||
      System.getProperty("scalaxy.loops.optimized") == "false"

    // This needs to be public and statically accessible.
    def rangeForeachImpl[U : c.WeakTypeTag](c: Context)(f: c.Expr[Int => U]): c.Expr[Unit] =
    {
      import c.universe._

      object Recompose {
        def unapply(tree: Tree): Option[Tree] = Option(tree) collect {
          case q"$_.rangeExtensions($target).optimized" =>
            target

          case q"${Recompose(target)}.withFilter($f)" =>
            c.typecheck(q"$target.withFilter($f)")

          case q"${Recompose(target)}.withFilter($f)" =>
            c.typecheck(q"$target.withFilter($f)")

          case q"${Recompose(target)}.filter($f)" =>
            c.typecheck(q"$target.filter($f)")

          case q"${Recompose(target)}.foreach[..$targs]($f)" =>
            c.typecheck(q"$target.foreach[..$targs]($f)")
        }
      }
      c.macroApplication match {
        case Recompose(target) =>
          if (disabled)
            c.Expr[Unit](target)
          else
            scalaxy.streams.impl.recursivelyOptimize(c)(c.Expr[Unit](c.typecheck(target)))

        case _ =>
          c.error(c.macroApplication.pos, "This is not supported anymore")
          null
      }
    }
  }
}
