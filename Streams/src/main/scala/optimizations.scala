package scalaxy.streams;

sealed class OptimizationStrategy(val name: String) {
  def fullName = getClass.getPackage.getName + ".optimization." + name
}

/**
 * Example:
 *   import scalaxy.streams.optimization.aggressive
 *   for (x <- Array(1, 2, 3); y = x * 10; z = y + 2) print(z)
 */
object optimization {
  implicit case object none extends OptimizationStrategy("none")
  /** Performs optimizations that don't alter any Scala semantics. */
  implicit case object safe extends OptimizationStrategy("safe")
  /** Performs unsafe rewrites. */
  implicit case object aggressive extends OptimizationStrategy("aggressive")
  /** Performs all possible rewrites, even those known to be slower or unsafe. */
  implicit case object foolish extends OptimizationStrategy("foolish")

  // /** Makes sure all possible lambdas are rewritten away. This may produce slower and unsafe code. */
  // implicit case object eliminateLambdas extends OptimizationStrategy("eliminateLambdas")

  private[this] val strategies =
    List(none, safe, aggressive, foolish)

  private[this] val strategyByName =
    strategies.map(s => (s.name, s)).toMap

  def forName(name: String) = strategyByName(name)

  val STRATEGY_PROPERTY = "scalaxy.streams.strategy"
  val STRATEGY_ENV_VAR = "SCALAXY_STREAMS_STRATEGY"

  implicit val default: OptimizationStrategy = aggressive

  private[this] val envVarOpt =
    Option(System.getenv(STRATEGY_ENV_VAR))
  private[this] def javaProp =
    Option(System.getProperty(STRATEGY_PROPERTY))

  implicit lazy val global: OptimizationStrategy =
    javaProp.orElse(envVarOpt).
      flatMap(strategyByName.get(_)).
      getOrElse(default)
}

