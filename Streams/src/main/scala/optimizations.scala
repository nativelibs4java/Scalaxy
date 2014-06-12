package scalaxy;

sealed trait optimization

object optimization {
  case object none extends optimization
  /** Performs optimizations that don't alter any Scala semantics. */
  case object safe extends optimization
  /** Performs unsafe rewrites. */
  case object aggressive extends optimization
  // /** Makes sure all possible lambdas are rewritten away. This may produce slower and unsafe code. */
  // case object eliminateLambdas extends optimization

  val default = aggressive
}

