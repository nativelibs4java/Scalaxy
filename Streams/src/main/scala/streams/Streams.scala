package scalaxy.streams

private[streams] trait Streams
    extends StreamComponents
    with UnusableSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeStream extends Extractor[Tree, Stream] {
    def findSink(ops: List[StreamComponent]): Option[StreamSink] = {
      ops.reverse.toIterator.zipWithIndex.map({
        case (op, i) => (op.sinkOption, i)
      }) collectFirst {
        case (Some(sink), i) if !sink.isFinalOnly || i == 0 =>
          sink
      }
    }

    def unapply(tree: Tree): Option[Stream] = tree match {
      case SomeStreamSink(SomeStreamOp(SomeStreamSource(source), ops), sink) =>
        Some(new Stream(source, ops, sink, hasExplicitSink = true))

      case SomeStreamOp(SomeStreamSource(source), ops) =>
        findSink(source :: ops).filter(_ != InvalidSink)
          .map(sink => new Stream(source, ops, sink, hasExplicitSink = false))

      case SomeStreamSource(source) =>
        findSink(List(source)).filter(_ != InvalidSink)
          .map(sink => new Stream(source, Nil, sink, hasExplicitSink = false))

      case _ =>
        None
    }
  }

  case class Stream(
      source: StreamSource,
      ops: List[StreamOp],
      sink: StreamSink,
      hasExplicitSink: Boolean)
  {
    def describe(describeSink: Boolean = true) =
      (source :: ops).flatMap(_.describe).mkString(".") +
      sink.describe.filter(_ => describeSink).map(" -> " + _).getOrElse("")

    val components: List[StreamComponent] = (source :: ops) :+ sink
    def lambdaCount: Int = components.map(_.lambdaCount).sum
    lazy val closureSideEffectss: List[List[SideEffect]] =
      components.flatMap(_.closureSideEffectss)

    // TODO: refine this.
    def isWorthOptimizing(strategy: OptimizationStrategy,
                          info: (Position, String) => Unit,
                          warning: (Position, String) => Unit) = {
      var reportedSideEffects = Set[SideEffect]()

      lazy val hasMoreThanOneLambdaWithUnsafeSideEffect =
        closureSideEffectss.filter(_.contains(SideEffectSeverity.Unsafe)).size > 1

      def reportIgnoredUnsafeSideEffects(): Unit = {
        if (hasMoreThanOneLambdaWithUnsafeSideEffect) {
          for (effects <- closureSideEffectss; effect <- effects; if effect.severity == SideEffectSeverity.Unsafe) {
            reportedSideEffects += effect
            warning(effect.tree.pos, Optimizations.messageHeader +
              s"Side effect might be cause issue with ${strategy.name} optimization strategy: ${effect.description}")
          }
        }
      }

      val result = strategy match {

        // TODO: List.map is not worth optimizing, because of its (unfair) hand-optimized implementation.
        // TODO: explain when veryVerbose
        case scalaxy.streams.optimization.safer =>
          // At least one lambda, at most one closure with any severity of side-effect.
          lambdaCount >= 1 &&
          closureSideEffectss.filter(_ != Nil).size <= 1

        case scalaxy.streams.optimization.safe =>
          // At least one lambda, at most one closure with Unsafe side-effects (allow ProbablySafe side-effects).
          lambdaCount >= 1 && !hasMoreThanOneLambdaWithUnsafeSideEffect

        case scalaxy.streams.optimization.aggressive =>
          // At least one lambda, warn if there is more than one closure with Unsafe side-effects.
          reportIgnoredUnsafeSideEffects()

          lambdaCount >= 1

        case scalaxy.streams.optimization.foolish =>
          // Optimize everything, even when there's no lambda at all (e.g. `(0 to 10).toList`), with same side-effect
          // warnings as for `aggressive`.
          reportIgnoredUnsafeSideEffects()

          ops.length > 0 || hasExplicitSink

        case _ =>
          assert(strategy == scalaxy.streams.optimization.none)
          false
      }

      if (impl.veryVerbose) {
        for (effects <- closureSideEffectss; effect <- effects; if !reportedSideEffects(effect)) {
          info(effect.tree.pos, Optimizations.messageHeader + s"Side effect: ${effect.description}")
        }
      }

      result
    }

    def emitStream(fresh: String => TermName,
                   transform: Tree => Tree,
                   typed: Tree => Tree,
                   currentOwner: Symbol,
                   untyped: Tree => Tree,
                   sinkNeeds: Set[TuploidPath] = sink.outputNeeds,
                   loopInterruptor: Option[Tree] = None): StreamOutput =
    {
      val sourceNeeds :: outputNeeds = ops.scanRight(sinkNeeds)({ case (op, refs) =>
        op.transmitOutputNeedsBackwards(refs)
      })
      val nextOps = ops.zip(outputNeeds) :+ (sink, sinkNeeds)
      // println(s"source = $source")
      // println(s"""ops =\n\t${ops.map(_.getClass.getName).mkString("\n\t")}""")
      // println(s"outputNeeds = ${nextOps.map(_._2)}")
      source.emit(
        input = StreamInput(
          vars = UnitTreeScalarValue,
          loopInterruptor = loopInterruptor,
          fresh = fresh,
          transform = transform,
          currentOwner = currentOwner,
          typed = typed,
          untyped = untyped),
        outputNeeds = sourceNeeds,
        nextOps = nextOps)
    }
  }
}
