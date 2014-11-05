package scalaxy.streams

private[streams] trait TakeDropOps
    extends ClosureStreamOps
    with Strippers
    with OptionSinks
    with UnusableSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeTakeDropOp extends StreamOpExtractor {

    private[this] def isOption(tree: Tree): Boolean =
      Option(tree.tpe).orElse(Option(tree.symbol).map(_.typeSignature)).
        exists(_ <:< typeOf[Option[_]])

    override def unapply(tree: Tree) = Option(tree).filter(!isOption(_)) collect {
      case q"$target.take($n)" =>
        (target, TakeOp(n))

      case q"$target.drop($n)" =>
        (target, DropOp(n))
    }
  }

  trait TakeDropOp extends StreamOp {
    def n: Tree
    override def subTrees = List(n)
    override def canAlterSize = true
    override def sinkOption = None
    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) = paths
  }

  case class TakeOp(n: Tree) extends TakeDropOp
  {
    override def canInterruptLoop = true
    override def describe = Some("take")

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input.{ typed, fresh, transform }

      val nn = fresh("nn")
      val i = fresh("i")

      // Force typing of declarations and get typed references to various vars and vals.
      val Block(List(
          nValDef,
          iVarDef,
          test,
          iIncr), iVarRef) = typed(q"""
        private[this] val $nn = ${transform(n)};
        private[this] var $i = 0;
        $i < $n;
        $i += 1;
        $i
      """)

      var sub = emitSub(input.copy(outputSize = None), nextOps)
      sub.copy(
        beforeBody = sub.beforeBody ++ List(nValDef, iVarDef),
        body = List(q"""
        if ($test) {
          ..${sub.body};
          $iIncr
        } else {
          ${input.loopInterruptor.get.duplicate} = false;
        }
      """))
    }
  }

  case class DropOp(n: Tree) extends TakeDropOp
  {
    override def canInterruptLoop = false
    override def describe = Some("drop")

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input.{ typed, fresh, transform }

      val nn = fresh("nn")
      val i = fresh("i")

      // Force typing of declarations and get typed references to various vars and vals.
      val Block(List(
          nValDef,
          iVarDef,
          test,
          iIncr), iVarRef) = typed(q"""
        private[this] val $nn = ${transform(n)};
        private[this] var $i = 0;
        $i < $n;
        $i += 1;
        $i
      """)

      var sub = emitSub(input.copy(outputSize = None), nextOps)
      sub.copy(
        beforeBody = sub.beforeBody ++ List(nValDef, iVarDef),
        body = List(q"""
        if ($test) {
          $iIncr
        } else {
          ..${sub.body};
        }
      """))
    }
  }
}
