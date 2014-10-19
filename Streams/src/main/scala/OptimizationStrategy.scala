package scalaxy.streams;

sealed class OptimizationStrategy(val name: String) {
  def fullName = getClass.getPackage.getName + ".strategy." + name
}

object OptimizationStrategy {
  case object none extends OptimizationStrategy("none")

  /** Performs optimizations that don't alter any Scala semantics, using strict
   * side-effect detection. */
  case object safer extends OptimizationStrategy("safer")

  /** Performs optimizations that don't alter any Scala semantics, using reasonably
   * optimistic side-effect detection (for instance, assumes hashCode / equals / toString
   * are side-effect-free for all objects. */
  case object safe extends OptimizationStrategy("safe")

  /** Performs unsafe rewrites, ignoring side-effect analysis (which may
   * alter the semantics of the code. */
  case object aggressive extends OptimizationStrategy("aggressive")

  /** Performs all possible rewrites, even those known to be slower or unsafe. */
  case object foolish extends OptimizationStrategy("foolish")

  // /** Makes sure all possible lambdas are rewritten away. This may produce slower and unsafe code. */
  // implicit case object eliminateLambdas extends OptimizationStrategy("eliminateLambdas")

  private[this] val strategies =
    List(none, safe, safer, aggressive, foolish)

  private[this] val strategyByName: Map[String, OptimizationStrategy] =
    strategies.map(s => (s.name, s)).toMap

  def forName(name: String): Option[OptimizationStrategy] =
    if (name == null || name == "") None
    else Some(strategyByName(name))
}

