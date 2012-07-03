package scalaxy.inlinable

import language.experimental.macros
import reflect.makro.Context

private[inlinable] object InlinableRangeMacros
{
  private[this] val errorPrefix = "Failed to optimize loop"

  def rangeForeachImpl(c: Context)(f: c.Expr[Int => Unit]): c.Expr[Unit] = {
    c.Expr[Unit](
      new RangeLoops {
        override val universe = c.universe
        import universe._
        import definitions._

        object StartEndInclusive {
          def unapply(tree: Tree): Option[(Int, Int, Boolean)] = Option(tree) collect {
            case
              Apply(
                Select(
                  Apply(
                    Select(_, intWrapperName()),
                    List(Literal(Constant(start: Int)))),
                  junction @ (toName() | untilName())),
                List(Literal(Constant(end: Int))))
            =>
              (start, end, toName.unapply(junction))
          }
        }

        lazy val defaultReplacement = {
          Apply(
            Select(
              Select(c.prefix.tree.asInstanceOf[Tree], "toRange"),
              "foreach"),
            List(f.tree.asInstanceOf[Tree]))
        }

        val result = c.typeCheck(f.tree).asInstanceOf[Tree] match {
          case Function(List(param @ ValDef(mods, name, tpt, rhs)), body) =>
            c.prefix.tree.asInstanceOf[Tree] match {
              // TODO handle non-constant cases!
              case
                StartEndInclusive(start, end, isInclusive)
              =>
                newWhileRangeLoop(c.fresh(_), start, end, step = 1, isInclusive, param, body)
              case
                Apply(
                  Select(
                    StartEndInclusive(start, end, isInclusive),
                    byName()),
                  List(Literal(Constant(step: Int))))
              =>
                newWhileRangeLoop(c.fresh(_), start, end, step, isInclusive, param, body)
              case _ =>
                c.warning(c.prefix.tree.pos, errorPrefix + " (unsupported range: " + c.prefix.tree + ")")
                defaultReplacement
            }
          case _ =>
            c.warning(f.tree.pos, errorPrefix + " (unsupported body function: " + f.tree + ")")
            defaultReplacement
        }
      }.result.asInstanceOf[c.Tree]
    )
  }
}

