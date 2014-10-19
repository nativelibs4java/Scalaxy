package scalaxy.streams;

/**
 * Example:
 *   import scalaxy.streams.strategy.aggressive
 *   for (x <- Array(1, 2, 3); y = x * 10; z = y + 2) print(z)
 */
object strategy {
  val none = OptimizationStrategy.none
  val safer = OptimizationStrategy.safer
  val safe = OptimizationStrategy.safe
  val aggressive = OptimizationStrategy.aggressive
  val foolish = OptimizationStrategy.foolish

  val default: OptimizationStrategy = safe

  private[streams] lazy val global: OptimizationStrategy =
    javaProp.orElse(envVarOpt).
      flatMap(OptimizationStrategy.forName).
      getOrElse(default)

  private[this] val STRATEGY_PROPERTY = "scalaxy.streams.strategy"
  private[this] val STRATEGY_ENV_VAR = "SCALAXY_STREAMS_STRATEGY"

  private[this] val envVarOpt =
    Option(System.getenv(STRATEGY_ENV_VAR))
  private[this] def javaProp =
    Option(System.getProperty(STRATEGY_PROPERTY))
}

