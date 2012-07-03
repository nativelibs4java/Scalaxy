package scalaxy

package object inlinable
{
  implicit def InlinableRange2Range(r: InlinableRange) =
    r.toRange

  // TODO merge with scala.runtime.RichInt (and use `to`, `until`)
  // TODO implement to(end, step) and until(end, step)
  class RichInt(self: Int)
  {
    def to_(end: Int) =
      InlinableRange(self, end, 1, true)

    def until_(end: Int) =
      InlinableRange(self, end, 1, false)
  }

  implicit def intWrapper(i: Int) =
    new RichInt(i)

  /*
  // TODO have RichInt not to be final, to allow this:
  // import Predef.{ intWrapper => _ }

  class RichInt2(val self: Int) extends scala.runtime.RichInt(self) {
    override def to(end: Int) =
      InlinableRange(self, end, 1, true)
    override def until(end: Int) =
      InlinableRange(self, end, 1, false)
    override def to(end: Int, step: Int) =
      InlinableRange(self, end, step, true)
    override def until(end: Int, step: Int) =
      InlinableRange(self, end, step, false)
  }
  implicit def richInt2ToRichInt(i: RichInt2) = new scala.runtime.RichInt(i.self)
  implicit def intWrapper(i: Int) = new RichInt2(i)
  */

  /*
  // For syntax: `0 to 10 inlined`
  implicit def Range2InlinableRange(r: Range) = new {
    def inlined = InlinableRange(r.start, r.end, r.step, r.isInclusive)
  }
  */
}
