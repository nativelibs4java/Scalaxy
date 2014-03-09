package scalaxy.loops

private[loops] trait FlatMapOps
    extends ClosureStreamOps
    with CanBuildFromSinks
{
  val global: scala.reflect.api.Universe
  import global._

  val SomeStream: Extractor[Tree, Stream]

  object SomeFlatMapOp {
    def unapply(tree: Tree): Option[(Tree, StreamOp)] = Option(tree) collect {
      case q"$target.flatMap[$tpt, $_](${fun @ Strip(Function(List(param), body))})($canBuildFrom)" =>
        (target, FlatMapOp(tpt.tpe, param, body, canBuildFrom))
        // (target, body match {
        //   case SomeStream(stream) =>
        //     NestedFlatMapOp(tpt.tpe, param, stream, canBuildFrom)

        //   case _ =>
        //     // println(s"""
        //     //   GenericFlatMapOp {
                
        //     //     body: $body

        //     //     fun: $fun

        //     //     tree: $tree
        //     //   }
        //     // """)
        //     GenericFlatMapOp(tpt.tpe, param, body, canBuildFrom)
        // })
    }
  }

  case class FlatMapOp(tpe: Type, param: ValDef, body: Tree, canBuildFrom: Tree)
      extends ClosureStreamOp
  {
    val nestedStream: Option[Stream] = q"($param) => $body" match {
      case SomeTransformationClosure(TransformationClosure(_, Nil, ScalarValue(_, Some(SomeStream(stream)), _))) =>
        Some(stream)

      case _ =>
        None // TODO
    }

    override def describe = Some(
      "flatMap" + nestedStream.map("(" + _.describe(describeSink = false) + ")").getOrElse(""))

    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) = {
      super.transmitOutputNeedsBackwards(paths) ++
      nestedStream.toList.flatMap(_.ops.foldRight(paths)({ case (op, refs) =>
        op.transmitOutputNeedsBackwards(refs)
      }))
    }

    override val sinkOption = Some(CanBuildFromSink(canBuildFrom))

    override def emitOp(
        inputVars: TuploidValue[Tree],
        outputNeeds: Set[TuploidPath],
        opsAndOutputNeeds: List[(StreamOp, Set[TuploidPath])],
        fresh: String => TermName,
        transform: Tree => Tree): StreamOpResult =
    {
      nestedStream match {
        case Some(stream) =>
          val replacer = transformationClosure.getReplacer(inputVars)
          val subTransformer = new Transformer {
            override def transform(tree: Tree) = {
              if (tree.symbol == param.symbol) {
                inputVars.alias.get.duplicate
              } else {
                super.transform(tree)
              }
            }
          }
          val subTransform = (tree: Tree) => subTransformer.transform(transform(tree))
          val SinkOp(outerSink) :: outerOpsRev = opsAndOutputNeeds.map(_._1).reverse
          val outerOps = outerOpsRev.reverse
          val modifiedStream = stream.copy(ops = stream.ops ++ outerOps, sink = outerSink)

          modifiedStream.emitStream(fresh, subTransform).map(replacer)

        case None =>
          // TODO: type this.
          ???
          val itemVal = fresh("item")
          // println("GenericFlatMapOp(tpe = " + tpe + ")")
          val (replacedStatements, outputVars) = transformationClosure.replaceClosureBody(
            inputVars = ScalarValue(tpe, alias = Some(Ident(itemVal.toString))),
            outputNeeds, fresh, transform)
          val StreamOpResult(streamPrelude, streamBody, streamEnding) =
            emitSub(outputVars, opsAndOutputNeeds, fresh, transform)

          StreamOpResult(
            prelude = streamPrelude,
            body = List(typed(q"""
              ..$replacedStatements;
              for ($itemVal <- ${outputVars.alias.get}) {
                ..$streamBody;
              }
            """)),
            ending = streamEnding
          )
      }
    }
  }

  // case class GenericFlatMapOp(tpe: Type, param: ValDef, body: Tree, canBuildFrom: Tree)
  //     extends ClosureStreamOp
  // {
  //   override def describe = Some("flatMap")

  //   override val sinkOption = Some(CanBuildFromSink(canBuildFrom))

  //   override def emitOp(
  //       inputVars: TuploidValue[Tree],
  //       outputNeeds: Set[TuploidPath],
  //       opsAndOutputNeeds: List[(StreamOp, Set[TuploidPath])],
  //       fresh: String => TermName,
  //       transform: Tree => Tree): StreamOpResult =
  //   {
  //     // TODO: type this.
  //     val itemVal = fresh("item")
  //     println("GenericFlatMapOp(tpe = " + tpe + ")")
  //     val (replacedStatements, outputVars) = transformationClosure.replaceClosureBody(
  //       inputVars = ScalarValue(tpe, alias = Some(Ident(itemVal.toString))),
  //       outputNeeds, fresh, transform)
  //     val StreamOpResult(streamPrelude, streamBody, streamEnding) =
  //       emitSub(outputVars, opsAndOutputNeeds, fresh, transform)

  //     StreamOpResult(
  //       prelude = streamPrelude,
  //       body = List(typed(q"""
  //         ..$replacedStatements;
  //         for ($itemVal <- ${outputVars.alias.get}) {
  //           ..$streamBody;
  //         }
  //       """)),
  //       ending = streamEnding
  //     )
  //   }
  // }

  // case class NestedFlatMapOp(tpe: Type, param: ValDef, subStream: Stream, canBuildFrom: Tree)
  //     extends StreamOp
  // {
  //   override def describe = Some("flatMap(" + subStream.describe(describeSink = false) + ")")

  //   override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) = {
  //     subStream.ops.foldRight(paths)({ case (op, refs) =>
  //       op.transmitOutputNeedsBackwards(refs)
  //     })
  //   }

  //   override val sinkOption = Some(CanBuildFromSink(canBuildFrom))

  //   override def emitOp(
  //       inputVars: TuploidValue[Tree],
  //       outputNeeds: Set[TuploidPath],
  //       opsAndOutputNeeds: List[(StreamOp, Set[TuploidPath])],
  //       fresh: String => TermName,
  //       transform: Tree => Tree): StreamOpResult =
  //   {
  //     val subTransformer = new Transformer {
  //       override def transform(tree: Tree) = {
  //         if (tree.symbol == param.symbol) {
  //           inputVars.alias.get.duplicate
  //         } else {
  //           super.transform(tree)
  //         }
  //       }
  //     }
  //     val subTransform = (tree: Tree) => subTransformer.transform(transform(tree))
  //     val Stream(source, ops, sink) = subStream
  //     val SinkOp(outerSink) :: outerOpsRev = opsAndOutputNeeds.map(_._1).reverse
  //     val outerOps = outerOpsRev.reverse
  //     Stream(source, ops ++ outerOps, outerSink).emitStream(fresh, subTransform)
  //   }
  // }

}
