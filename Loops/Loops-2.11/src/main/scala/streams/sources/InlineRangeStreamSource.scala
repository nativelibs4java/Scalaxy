package scalaxy.loops
import scala.reflect.NameTransformer.{ encode, decode }

private[loops] trait InlineRangeStreamSources extends Streams with StreamSources {
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

  object SomeInlineRangeStreamSource extends Extractor[Tree, InlineRangeStreamSource[_]]
  {
    def unapply(tree: Tree): Option[InlineRangeStreamSource[_]] = Option(tree) collect {
      case q"scala.this.Predef.intWrapper($start) ${ToUntil(isInclusive)} $end" =>
        InlineRangeStreamSource[Int](start, end, by = 1, isInclusive, tpe = typeOf[Int])

      case q"scala.this.Predef.intWrapper($start) ${ToUntil(isInclusive)} $end by ${Literal(Constant(by: Int))}" =>
        InlineRangeStreamSource[Int](start, end, by, isInclusive, tpe = typeOf[Int])

      case q"scala.this.Predef.longWrapper($start) ${ToUntil(isInclusive)} $end" =>
        InlineRangeStreamSource[Long](start, end, by = 1, isInclusive, tpe = typeOf[Long])

      case q"scala.this.Predef.longWrapper($start) ${ToUntil(isInclusive)} $end by ${Literal(Constant(by: Long))}" =>
        InlineRangeStreamSource[Long](start, end, by, isInclusive, tpe = typeOf[Long])
    }
  }

  case class InlineRangeStreamSource[T : Numeric : Liftable]
    (start: Tree, end: Tree, by: T, isInclusive: Boolean, tpe: Type)
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

      val testOperator: TermName =
        encode(
          if (implicitly[Numeric[T]].signum(by) > 0) {
            if (isInclusive) "<=" else "<"
          } else {
            if (isInclusive) ">=" else ">"
          }
        )

      // Force typing of declarations and get typed references to various vars and vals.
      val b @ Block(List(startValDef, endValDef, iVarDef, iValDef, iValRef, iVarRef, endValRef, test, iVarIncr), _) = typed(q"""
        private[this] val $startVal: $tpe = ${transform(start)};
        private[this] val $endVal: $tpe = ${transform(end)};
        private[this] var $iVar: $tpe = $startVal;
        private[this] val $iVal: $tpe = $iVar;
        $iVal;
        $iVar;
        $endVal;
        $iVar $testOperator $endVal;
        $iVar += $by;
        {}
      """)

      println("FAKE TYPED BLOCK: " + b)

      val outputVars = ScalarValue[Tree](tpe = tpe, alias = Some(iValRef))

      val StreamOpResult(streamPrelude, streamBody, streamEnding) =
        emitSub(outputVars, opsAndOutputNeeds, fresh, transform)


      // q"""
      //   private[this] val $startVal = ${transform(start)}
      //   private[this] val $endVal = ${transform(end)}
      //   private[this] var $iVar = $startVal

      //   ..$streamPrelude
      //   while ($iVar $testOperator $endVal) {
      //     val $iVal = $iVar
      //     ..$streamBody
      //     $iVar += $by
      //   }
      //   ..$streamEnding
      // """

      val r = typed(q"""
        $startValDef
        $endValDef
        $iVarDef
        ..$streamPrelude;
        while ($test) {
          $iValDef;
          ..$streamBody;
          $iVarIncr
        }
        ..$streamEnding
      """)

      println("RESULT: " + r)
      r
    }
  }
}
