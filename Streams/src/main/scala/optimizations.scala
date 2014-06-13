package scalaxy.streams;

sealed trait OptimizationStrategy

object optimization {
  implicit case object none extends OptimizationStrategy
  /** Performs optimizations that don't alter any Scala semantics. */
  implicit case object safe extends OptimizationStrategy
  /** Performs unsafe rewrites. */
  implicit case object aggressive extends OptimizationStrategy

  // /** Makes sure all possible lambdas are rewritten away. This may produce slower and unsafe code. */
  // implicit case object eliminateLambdas extends optimization

  implicit val default: OptimizationStrategy = safe
  //val default = aggressive
}

