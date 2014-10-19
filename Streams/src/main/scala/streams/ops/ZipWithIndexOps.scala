package scalaxy.streams

import scala.reflect.NameTransformer.{ encode, decode }

private[streams] trait ZipWithIndexOps
  extends TransformationClosures
  with CanBuildFromSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeZipWithIndexOp extends StreamOpExtractor {
    override def unapply(tree: Tree) = Option(tree) collect {
      case q"$target.zipWithIndex[${_}, ${_}]($canBuildFrom)" =>
        (target, ZipWithIndexOp(canBuildFrom))
    }
  }

  case class ZipWithIndexOp(canBuildFrom: Tree) extends StreamOp
  {
    override def describe = Some("zipWithIndex")

    override def canAlterSize = false

    override val sinkOption = Some(CanBuildFromSink(canBuildFrom))

    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) = {
      paths collect {
        // Only transmit _._1 and its children backwards
        case 0 :: sub =>
          sub
      }
    }

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input.{ fresh, transform, typed }

      // TODO wire input and output fiber vars
      val indexVar = fresh("indexVar")
      val indexVal = fresh("indexVal")

      val needsPair: Boolean = outputNeeds(RootTuploidPath)
      val pairName = if (needsPair) fresh("zipWithIndexPair") else TermName("")

      // Early typing / symbolization.
      val Block(List(
          indexVarDef,
          indexVarRef,
          indexVarIncr,
          indexValDef,
          indexValRef,
          pairDef,
          pairRef), _) = typed(q"""
        private[this] var $indexVar = 0;
        $indexVar;
        $indexVar += 1;
        private[this] val $indexVal = $indexVar;
        $indexVal;
        private[this] val $pairName = (${input.vars.alias.getOrElse(EmptyTree)}, $indexVal);
        $pairName;
        ()
      """)

      import compat._
      val TypeRef(pre, sym, List(_, _)) = typeOf[(Int, Int)]
      val tupleTpe = internal.typeRef(pre, sym, List(input.vars.tpe, typeOf[Int]))
      require(tupleTpe != null && tupleTpe != NoType)
      val outputVars =
        TupleValue[Tree](
          tpe = tupleTpe,
          Map(
            0 -> input.vars,
            1 -> ScalarValue(typeOf[Int], alias = Some(indexValRef))),
          alias = Some(pairRef),
          couldBeNull = false)

      val sub = emitSub(input.copy(vars = outputVars), nextOps)
      sub.copy(
        // TODO pass source collection to canBuildFrom if it exists.
        beforeBody = sub.beforeBody :+ indexVarDef,
        body = List(q"""
          $indexValDef;
          ..${if (needsPair) List(pairDef) else Nil}
          ..${sub.body};
          $indexVarIncr
        """))
    }
  }
}
