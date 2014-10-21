package scalaxy.streams

private[streams] trait Strategies
    extends Streams
    with SideEffectsDetection
{
  self: StreamTransforms =>

  val global: scala.reflect.api.Universe
  import global._

  // TODO: refine this.
  def isWorthOptimizing(stream: Stream,
                        strategy: OptimizationStrategy,
                        info: (Position, String) => Unit,
                        warning: (Position, String) => Unit) = {
    var reportedSideEffects = Set[SideEffect]()

    val safeSeverities: Set[SideEffectSeverity] = strategy match {
      case scalaxy.streams.strategy.none |
           scalaxy.streams.strategy.safer =>
        Set()
      case scalaxy.streams.strategy.safe =>
        Set(SideEffectSeverity.ProbablySafe)
      case scalaxy.streams.strategy.aggressive |
           scalaxy.streams.strategy.foolish =>
        Set(SideEffectSeverity.ProbablySafe, SideEffectSeverity.Unsafe)
    }

    // println(s"safeSeverities(strategy: $strategy) = $safeSeverities")

    def hasUnsafeEffect(effects: List[SideEffect]): Boolean =
      effects.exists(e => !safeSeverities(e.severity))

    lazy val hasMoreThanOneLambdaWithUnsafeSideEffect =
      stream.closureSideEffectss.count(hasUnsafeEffect) > 1

    def couldSkipSideEffects: Boolean = {
      var foundCanInterruptLoop = false
      for (op <- stream.ops.reverse) {
        if (op.canInterruptLoop) {
          foundCanInterruptLoop = true
        } else {
          if (foundCanInterruptLoop &&
              hasUnsafeEffect(op.closureSideEffectss.flatten)) {
            return true
          }
        }
      }
      return false
    }

    def reportIgnoredUnsafeSideEffects(): Unit = if (!impl.quietWarnings) {
      for (effects <- stream.closureSideEffectss;
           effect <- effects;
           if effect.severity == SideEffectSeverity.Unsafe) {
        reportedSideEffects += effect
        warning(effect.tree.pos, Optimizations.messageHeader +
          s"Potential side effect could cause issues with ${strategy.name} optimization strategy: ${effect.description}")
      }
    }

    def isKnownNotToBeWorthOptimizing = stream match {
      //case Stream(_, ListStreamSource(_, _, _), List(Map(_, _) | Filter(_, _, _)), _) =>
      case Stream(_, ListStreamSource(_, _, _), _, _, _) if stream.lambdaCount == 1 =>
        // List operations are now quite heavily optimized. It only makes sense to
        // rewrite more than one operation.
        true

      case Stream(_,
          ArrayStreamSource(_, _, _),
          List(ArrayOpsOp, (TakeWhileOp(_, _) | DropWhileOp(_, _))), _, _) =>
        // Array.takeWhile / .dropWhile needs to be optimized better :-)
        true

      case Stream(_, source, ops, sink, _) =>
        // println(s"""
        //   NAH:
        //     source: $source
        //     ops: $ops
        //     sink: $sink
        // """)
        false
    }

    val worthOptimizing = strategy match {

      // TODO: List.map is not worth optimizing, because of its (unfair) hand-optimized implementation.
      // TODO: explain when veryVerbose
      case scalaxy.streams.strategy.safer |
           scalaxy.streams.strategy.safe =>
        // At least one lambda, at most one closure with unsafe side-effects.
        // For safer mode, ProbablySafe are treated as Unsafe.

        !isKnownNotToBeWorthOptimizing &&
        stream.lambdaCount >= 1 &&
        stream.closureSideEffectss.count(hasUnsafeEffect) <= 1 &&
        !couldSkipSideEffects

      case scalaxy.streams.strategy.aggressive =>
        // At least one lambda, warn if there is more than one closure with Unsafe side-effects.
        reportIgnoredUnsafeSideEffects()

        !isKnownNotToBeWorthOptimizing &&
        stream.lambdaCount >= 1

      case scalaxy.streams.strategy.foolish =>
        // Optimize everything, even when there's no lambda at all (e.g. `(0 to 10).toList`), with same side-effect
        // warnings as for `aggressive`.
        reportIgnoredUnsafeSideEffects()

        stream.ops.length > 0 ||
        stream.hasExplicitSink

      case _ =>
        assert(strategy == scalaxy.streams.strategy.none)
        false
    }

    if (impl.veryVerbose) {
      for (effects <- stream.closureSideEffectss;
           effect <- effects;
           if !reportedSideEffects(effect)) {
        info(effect.tree.pos, Optimizations.messageHeader + s"Side effect: ${effect.description} (${effect.severity.description})")
      }
    }

    // println(s"tree = ${stream.tree}\n\tstream = ${stream.describe()}\n\tstrategy = $strategy\n\tlambdaCount = ${stream.lambdaCount}\n\tclosureSideEffectss = ${stream.closureSideEffectss}\n\tcouldSkipSideEffects = $couldSkipSideEffects\n\thasMoreThanOneLambdaWithUnsafeSideEffect = $hasMoreThanOneLambdaWithUnsafeSideEffect\n\tisWorthOptimizing = $worthOptimizing")

    worthOptimizing
  }
}
