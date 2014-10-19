package scalaxy.streams

private[streams] trait FlatMapOps
    extends ClosureStreamOps
    with CanBuildFromSinks
    with Streams
    with Strippers
{
  val global: scala.reflect.api.Universe
  import global._

  val SomeStream: Extractor[Tree, Stream]

  lazy val GenTraversableOnceSym = rootMirror.staticClass("scala.collection.GenTraversableOnce")
  lazy val OptionModule = rootMirror.staticModule("scala.Option")

  object Option2Iterable {
    def unapply(tree: Tree): Option[Tree] = Option(tree) collect {
      case q"$target.option2Iterable[${_}]($value)" if target.symbol == OptionModule =>
        value
    }
  }
  def stripOption2Iterable(tree: Tree): Tree = tree match {
    case Option2Iterable(value) => value
    case value => value
  }
  object SomeFlatMapOp extends StreamOpExtractor {
    override def unapply(tree: Tree) = Option(tree) collect {
      case q"$target.flatMap[$tpt, ${_}](${Closure(closure)})($cbf)" =>
        (target, FlatMapOp(tpt.tpe, closure, Some(cbf)))

      // Option.flatMap doesn't take a CanBuildFrom.
      case q"$target.flatMap[$tpt](${Closure(closure)})" =>
        (target, FlatMapOp(tpt.tpe, closure, None))
    }
  }

  case class FlatMapOp(tpe: Type, closure: Function, canBuildFrom: Option[Tree])
      extends ClosureStreamOp
  {
    override def stripBody(tree: Tree) = stripOption2Iterable(tree)

    val nestedStream: Option[Stream] = q"($param) => $body" match {
      case SomeTransformationClosure(
        TransformationClosure(
          _,
          Nil,
          ScalarValue(_, Some(SomeStream(stream)), _),
          _)) =>
        Some(stream)

      case _ =>
        None // TODO
    }

    override def describe = Some(
      "flatMap" + nestedStream.map("(" + _.describe(describeSink = false) + ")").getOrElse(""))

    override def lambdaCount = 1 + nestedStream.map(_.lambdaCount).getOrElse(0)

    override def subTrees =
      nestedStream.map(_.subTrees).
        getOrElse(super.subTrees)

    override def closureSideEffectss =
      nestedStream.map(_.closureSideEffectss).
        getOrElse(super.closureSideEffectss)

    // Do not leak the interruptibility out of a flatMap.
    override def canInterruptLoop = false//nestedStream.map(_.ops.exists(_.canInterruptLoop)).getOrElse(false)

    override def canAlterSize = true

    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) = {
      super.transmitOutputNeedsBackwards(paths) ++
      nestedStream.toList.flatMap(_.ops.foldRight(paths)({ case (op, refs) =>
        op.transmitOutputNeedsBackwards(refs)
      }))
    }

    override val sinkOption = canBuildFrom.map(CanBuildFromSink(_))

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input.{ fresh, transform, typed, untyped, currentOwner }

      nestedStream match {
        case Some(stream) =>
          val replacer = getReplacer(transformationClosure.inputs, input.vars)
          val subTransformer = new Transformer {
            override def transform(tree: Tree) = {
              if (tree.symbol == param.symbol) {
                input.vars.alias.get.duplicate
              } else {
                super.transform(tree)
              }
            }
          }
          val subTransform = (tree: Tree) => subTransformer.transform(transform(tree))
          val (outerSink: StreamSink) :: outerOpsRev = nextOps.map(_._1).reverse
          val outerOps = outerOpsRev.reverse
          val modifiedStream = stream.copy(ops = stream.ops ++ outerOps, sink = outerSink)

          modifiedStream.emitStream(
            fresh, subTransform,
            currentOwner = currentOwner,
            typed = typed, untyped = untyped,
            loopInterruptor = input.loopInterruptor).map(replacer)

        case None =>
          val (replacedStatements, outputVars) =
            transformationClosure.replaceClosureBody(
              input.copy(
                outputSize = None,
                index = None),
              outputNeeds)

          val TypeRef(_, _, List(componentTpe)) =
            outputVars.tpe.baseType(GenTraversableOnceSym)

          val itemVal = fresh("item")
          val Function(List(itemValDef @ ValDef(_, _, _, _)), itemValRef @ Ident(_)) = typed(q"""
            ($itemVal: $componentTpe) => $itemVal
          """)

          val sub = emitSub(
            input.copy(
              vars = ScalarValue(componentTpe, alias = Some(itemValRef)),
              outputSize = None,
              index = None),
            nextOps)
          // It's important to untype sub.body because local vars will now live inside the new lambda.
          sub.copy(body = List(typed(q"""
            ..$replacedStatements;
            ${outputVars.alias.get}.foreach(($itemValDef) => {
              ..${sub.body.map(untyped)};
            })
          """)))
      }
    }
  }
}
