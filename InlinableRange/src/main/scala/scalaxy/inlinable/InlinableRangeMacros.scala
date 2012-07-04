package scalaxy.inlinable

import language.experimental.macros
import reflect.makro.Context


private[inlinable] object InlinableRangeMacros
{
  private[this] val errorMessageFormat = "Failed to optimize loop (%s)"
  private[this] val successMessage = "Optimized this loop"
  
  def rangeForeachImpl(c: Context)(f: c.Expr[Int => Unit]): c.Expr[Unit] = {
    c.Expr[Unit](
      new RangeLoops {
        override val universe = c.universe
        import universe._
        import definitions._

        override def resetAllAttrs(tree: Tree): Tree =
          c.resetAllAttrs(tree.asInstanceOf[c.Tree]).asInstanceOf[Tree]
        
        lazy val defaultReplacement = {
          Apply(
            Select(
              Select(c.prefix.tree.asInstanceOf[Tree], "toRange"),
              "foreach"),
            List(f.tree.asInstanceOf[Tree]))
        }

        val result = try {
          c.typeCheck(f.tree).asInstanceOf[Tree] match {
            case Function(List(param @ ValDef(mods, name, tpt, rhs)), body) =>
              c.prefix.tree.asInstanceOf[Tree] match {
                case InlinableRangeTree(start, end, step, isInclusive) =>
                  val optimized = 
                    newWhileRangeLoop(c.fresh(_), start, end, step, isInclusive, param, body)
                  //c.info(c.enclosingPosition, successMessage, force = true)
                  optimized
                case _ =>
                  c.warning(c.prefix.tree.pos, errorMessageFormat.format("unsupported range: " + c.prefix.tree))
                  defaultReplacement
              }
            case _ =>
              c.warning(f.tree.pos, errorMessageFormat.format("unsupported body function: " + f.tree))
              defaultReplacement
          }
        } catch { case ex =>
          ex.printStackTrace
          c.warning(c.enclosingPosition, errorMessageFormat.format("internal error: " + ex))  
        }
      }.result.asInstanceOf[c.Tree]
    )
  }
}

