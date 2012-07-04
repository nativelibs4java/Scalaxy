package scalaxy.inlinable

import language.experimental.macros
import reflect.makro.Context

trait InlinableRangeMatchers
extends InlinableNames
{
  val universe: reflect.makro.Universe
  import universe._
  
  /**
   * Matches `start to/until end [by step]`
   */
  object InlinableRangeTree {
    def unapply(tree: Tree): Option[(Tree, Tree, Option[Tree], Boolean)] = Option(tree) collect {
      case StartEndInclusive(start, end, isInclusive) =>
        (start, end, None, isInclusive)
      case StartEndStepInclusive(start, end, step, isInclusive) =>
        (start, end, Some(step), isInclusive)
    }
  }
  
  private[this] object StartEndInclusive {
    def unapply(tree: Tree): Option[(Tree, Tree, Boolean)] = Option(tree) collect {
      case
        Apply(
          Select(
            Apply(
              Select(_, intWrapperName()),
              List(start)),
            junction @ (toName() | untilName())),
          List(end))
      =>
        (start, end, toName.unapply(junction))
    }
  }
  private[this] object StartEndStepInclusive {
    def unapply(tree: Tree): Option[(Tree, Tree, Tree, Boolean)] = Option(tree) collect {
      case
        Apply(
          Select(
            StartEndInclusive(start, end, isInclusive),
            byName()),
          List(step))
      =>
        (start, end, step, isInclusive)
    }
  }
}
