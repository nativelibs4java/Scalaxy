package scalaxy.loops

private[loops] trait InlineRangeStreamSources extends Streams {
  val global: scala.reflect.api.Universe
  import global._

  object ToUntil
  {
    def apply(isInclusive: Boolean) = ???
    def unapply(name: Name): Option[Boolean] = if (name == null) None else name.toString match {
      case "to" => Some(true)
      case "until" => Some(false)
      case _ => None
    }
  }

  object SomeInlineRangeStreamSource
  {
    def unapply(tree: Tree): Option[InlineRangeStreamSource[_]] = Option(tree) collect {
      case q"scala.this.Predef.intWrapper($start) ${ToUntil(isInclusive)} $end" =>
        InlineRangeStreamSource[Int](start, end, by = 1, isInclusive)

      case q"scala.this.Predef.intWrapper($start) ${ToUntil(isInclusive)} $end by ${Literal(Constant(by: Int))}" =>
        InlineRangeStreamSource[Int](start, end, by, isInclusive)

      case q"scala.this.Predef.longWrapper($start) ${ToUntil(isInclusive)} $end" =>
        InlineRangeStreamSource[Long](start, end, by = 1, isInclusive)

      case q"scala.this.Predef.longWrapper($start) ${ToUntil(isInclusive)} $end by ${Literal(Constant(by: Long))}" =>
        InlineRangeStreamSource[Long](start, end, by, isInclusive)
    }
  }

  case class InlineRangeStreamSource[T <: AnyVal : Numeric : Liftable]
    (start: Tree, end: Tree, by: T, isInclusive: Boolean)
      extends StreamSource
  {
    override def sinkOption = {
      println("TODO vector / range sink")
      None
    }

    override def emitSource(
        outputNeeds: Set[TuploidPath],
        opsAndOutputNeeds: List[(StreamOp, Set[TuploidPath])],
        fresh: String => TermName,
        transform: Tree => Tree): Tree =
    {
      val startVal = fresh("start")
      val endVal = fresh("end")
      val iVar = fresh("i")
      val iVal = fresh("iVal")

      val outputVars = ScalarValue[TermName](alias = Some(iVal))

      val StreamOpResult(streamPrelude, streamBody, streamEnding) =
        emitSub(outputVars, opsAndOutputNeeds, fresh, transform)

      val testOperator: TermName =
        if (implicitly[Numeric[T]].signum(by) > 0) {
          if (isInclusive) "<=" else "<"
        } else {
          if (isInclusive) ">=" else ">"
        }

      q"""
        val $startVal = ${transform(start)}
        val $endVal = ${transform(end)}
        var $iVar = $startVal

        ..$streamPrelude
        while ($iVar $testOperator $endVal) {
          val $iVal = $iVar
          ..$streamBody
          $iVar += $by
        }
        ..$streamEnding
      """
    }
  }
}
