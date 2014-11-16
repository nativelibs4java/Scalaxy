package scalaxy.streams

import scala.language.existentials

private[streams] trait Strategies
    extends Streams
    with SideEffectsDetection
    with Reporters
{
  self: StreamTransforms =>

  val global: scala.reflect.api.Universe
  import global._

  def hasKnownLimitationOrBug(stream: Stream): Boolean = {

    def hasTakeOrDrop: Boolean = stream.ops.exists({
      case TakeWhileOp(_, _) | DropWhileOp(_, _) | TakeOp(_) | DropOp(_) =>
        true

      case _ =>
        false
    })

    // Detects two potentially-related issues.
    def hasTryOrByValueSubTrees: Boolean = stream.components.exists(_.subTrees.exists {
      case Try(_, _, _) =>
        // This one is... interesting.
        // Something horrible (foo not found) happens to the following snippet in lambdalift:
        //
        //     val msg = {
        //       try {
        //         val foo = 10
        //         Some(foo)
        //       } catch {
        //         case ex: Throwable => None
        //       }
        //     } get;
        //     msg
        //
        // I'm being super-mega cautious with try/catch here, until the bug is understood / fixed.
        true

      case t @ Apply(target, args)
          if Option(t.symbol).exists(_.isMethod) =>
        // If one of the subtrees is a method call with by-name params, then
        // weird symbol ownership issues arise (x not found in the following snippet)
        //
        //     def wrap[T](body: => T): Option[T] = Option(body)
        //     wrap({ val x = 10; Option(x) }) getOrElse 0
        //
        t.symbol.asMethod.paramLists.exists(_.exists(_.asTerm.isByNameParam))

      case _ =>
        false
    })

    def isRangeTpe(tpe: Type): Boolean =
      tpe <:< typeOf[Range] ||
      tpe <:< typeOf[collection.immutable.NumericRange[_]]

    def isOptionTpe(tpe: Type): Boolean =
      tpe <:< typeOf[Option[_]]

    def isWithFilterOp(op: StreamOp): Boolean = op match {
      case WithFilterOp(_) => true
      case _ => false
    }

    def streamTpe: Option[Type] = findType(stream.tree)

    stream.source match {
      case RangeStreamSource(_) if hasTakeOrDrop && streamTpe.exists(isRangeTpe) =>
        // Range.take / drop / takeWhile / dropWhile return Ranges: not handled yet.
        true

      case OptionStreamSource(_) if hasTakeOrDrop && streamTpe.exists(isOptionTpe) =>
        // Option.take / drop / takeWhile / dropWhile return Lists: not handled yet.
        true

      case _ if stream.ops.lastOption.exists(isWithFilterOp) =>
        // Option.withFilter returns an Option#WithFilter
        true

      case _ if hasTryOrByValueSubTrees =>
        true

      case _ =>
        false
    }
  }

  // TODO: refine this.
  def isWorthOptimizing(stream: Stream,
                        strategy: OptimizationStrategy) = {
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
        if (op.canInterruptLoop || op.canAlterSize) {
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

    def hasTakeOrDropWhileOp: Boolean = stream.ops.exists({
      case TakeWhileOp(_, _) | DropWhileOp(_, _) => true
      case _ => false
    })
    def isKnownNotToBeWorthOptimizing = stream match {
      //case Stream(_, ListStreamSource(_, _, _), List(Map(_, _) | Filter(_, _, _)), _) =>
      case Stream(_, ListStreamSource(_, _, _), _, _, _)
          if stream.lambdaCount == 1 =>
        // List operations are now quite heavily optimized. It only makes sense to
        // rewrite more than one operation.
        true

      case Stream(_, ArrayStreamSource(_, _, _), ops, _, _)
          if stream.lambdaCount == 1 &&
             hasTakeOrDropWhileOp =>
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

  def logException(pos: Position, ex: Throwable,
                   warning: (Position, String) => Unit) = {
    warning(pos, Optimizations.messageHeader + "An exception ocurred: " + ex)
    if (impl.veryVerbose) {
      ex.printStackTrace()
    }
  }
}
