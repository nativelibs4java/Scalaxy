package scalaxy.streams

private[streams] trait CollectOps
    extends ClosureStreamOps
    with CanBuildFromSinks
    with Strippers
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeCollectOp extends StreamOpExtractor {
    override def unapply(tree: Tree) = tree match {
      case q"""$target.collect[$outputTpt](${
        Strip(
          Block(
            List(
              cls @ ClassDef(_, _, _,
                Template(_, _, classBody))),
            _))
      })""" =>

        println(s"cls = $cls")
        classBody.collectFirst {
          case DefDef(_, name, _, _, _, Match(selector, cases))
              if name.toString == "applyOrElse" =>
            (target, CollectOp(outputTpt.tpe, cases, None))
        }

      case _ =>
        None
    }
  }

  // def mapLast[A, B](list: List[A])(f: A => B): List[B] = {
  //   val last :: others = list.reverse
  //   (f(last) :: others).reverse
  // }

  case class CollectOp(outputTpe: Type, cases: List[CaseDef], canBuildFrom: Option[Tree]) extends StreamOp {
    override def canInterruptLoop = false
    override def canAlterSize = true
    override def lambdaCount = 1
    override val sinkOption = canBuildFrom.map(CanBuildFromSink(_))
    override def describe = Some("collect")
    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) =
      Set(RootTuploidPath)

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input.{ typed, untyped, fresh }

      // val tpe = input.vars.tpe

      val value = fresh("value")
      val collected = fresh("collected")

      // Force typing of declarations and get typed references to various vars and vals.
      val Block(List(
          collectedVarDef,
          valueVarDef,
          collectedFalse,
          collectedVarRef,
          valueVarRef), _) = typed(q"""
        private[this] var $collected = true;
        private[this] var $value: $outputTpe =
          ${Literal(Constant(defaultValue(input.vars.tpe)))};
        $collected = false;
        $collected;
        $value;
        ""
      """)

      val caseUntyper = new Transformer {
        override def transform(tree: Tree) = tree match {
          // case Ident(n) =>
          //   println("FOUND IDENT " + tree + "; sym = " + tree.symbol)
          //   untyped(tree)
          case Bind(name, body) =>
            // Bind(name, body)//transform(body))
            typed(untyped(tree))
          case _ =>
            // println("FOUND a " + tree.getClass.getSimpleName + ": " + tree)
            super.transform(tree)
        }
      }

      //val untypedCases = cases.map(caseUntyper.transform(_))
      val untypedCases = cases
      // val untypedCases = cases.map(untyped)
      val matchCode = typed(Match(input.vars.alias.get.duplicate,
        untypedCases.dropRight(1).map({
          case CaseDef(pat, guard, caseValue) =>
            CaseDef(pat, guard, q"$valueVarRef = $caseValue")
        }) :+
        (untypedCases.last match {
          case CaseDef(pat, guard, _) =>
            // This is the default
            CaseDef(pat, guard, collectedFalse)
        })
      ))

      // TODO: use TransformationClosure to flatten tuples.

      val sub = emitSub(
        input.copy(
          vars = ScalarValue(outputTpe, alias = Some(valueVarRef)),
          outputSize = None,
          index = None),
        nextOps)
      // ..${sub.body.map(untyped)};
      sub.copy(body = List(typed(q"""
        $collectedVarDef;
        $valueVarDef;
        $matchCode;
        if ($collectedVarRef) {
          ..${sub.body};
        }
      """)))
    }
  }
}
