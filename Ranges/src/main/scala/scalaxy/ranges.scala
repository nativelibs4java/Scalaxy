package scalaxy

import scala.language.experimental.macros

import scala.reflect.macros.Context

// for (i <- 0 until n optimized) { ... }
package object ranges 
{
  implicit def rangeExtensions(range: Range) = new {
    def optimized: OptimizedRange =
      new OptimizedRange(range)
  }
}

package ranges
{
  class OptimizedRange(range: Range) 
  {
    def foreach[U](f: Int => U): Unit =
      macro impl.foreachImpl[U]
  } 

  package object impl
  {
    // This needs to be public and statically accessible.
    def foreachImpl[U : c.WeakTypeTag]
        (c: Context)
        (f: c.Expr[Int => U]): c.Expr[Unit] =
    {
      import c.universe._
    
      object InlinableRangeTree {
        def unapply(tree: Tree) = Option(tree) collect {
          case StartEndInclusive(start, end, isInclusive) =>
            (start, end, None, isInclusive)
          case StartEndStepInclusive(start, end, step, isInclusive) =>
            (start, end, Some(step), isInclusive)
        }
      }
    
      object StartEndInclusive {
        def unapply(tree: Tree) = Option(tree) collect {
          case
            Apply(
              Select(
                Apply(
                  Select(_, intWrapperName),
                  List(start)
                ),
                junctionName
              ),
              List(end)
            )
          if intWrapperName.toString == "intWrapper" &&
             (junctionName.toString == "to" || junctionName.toString == "until")
          =>
            (start, end, junctionName.toString == "to")
        }
      }
      object StartEndStepInclusive {
        def unapply(tree: Tree) = Option(tree) collect {
          case
            Apply(
              Select(
                StartEndInclusive(start, end, isInclusive),
                byName),
              List(step))
          if byName.toString == "by"
          =>
            (start, end, step, isInclusive)
        }
      }
      object IntConstant {
        def unapply(tree: Tree) = Option(tree) collect {
          case Literal(Constant(v: Int)) => v
        }
      }
      object PositiveIntConstant {
        def unapply(tree: Tree) = Option(tree) collect {
          case Literal(Constant(v: Int)) if v > 0 => v
        }
      }
      object NegativeIntConstant {
        def unapply(tree: Tree) = Option(tree) collect {
          case Literal(Constant(v: Int)) if v < 0 => v
        }
      }
      c.typeCheck(c.prefix.tree) match {
        case 
          Apply(
            TypeApply(
              Select(
                Apply(
                  Select(
                    Apply(_, List(range))
                  ), 
                  optimizedName
                ),
                foreachName
              ),
              List(u)
            ),
            List(f)
          )
        if optimizedName.toString == "optimized" &&
           foreachName.toString == "foreach"
        =>
          // TODO extract range and rewrite it
        case _ =>
          c.error(c.prefix.tree.pos, "Expression not recognized by the ranges macro.")
      }
      reify(())
    }
  }
}
