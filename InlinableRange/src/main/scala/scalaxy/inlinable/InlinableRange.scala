package scalaxy.inlinable

import language.experimental.macros
import reflect.makro.Context

case class InlinableRange(start: Int, end: Int, step: Int, isInclusive: Boolean)
{
  def toRange: Range = {
    if (isInclusive)
      Range.inclusive(start, end, step)
    else
      Range(start, end, step)
  }
  def by(step: Int) =
    copy(step = step)

  def foreach(f: Int => Unit): Unit =
    macro InlinableRangeMacros.rangeForeachImpl
}

