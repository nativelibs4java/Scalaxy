package scalaxy.streams;

private[streams] sealed trait SafetyCriteria
private[streams] object SafetyCriteria {
  case object Safe extends SafetyCriteria
  case object ProbablySafe extends SafetyCriteria
  case object Unsafe extends SafetyCriteria
}

private[streams] sealed trait SpeedupCriteria
private[streams] object SpeedupCriteria {
  case object Never extends SpeedupCriteria
  case object OnlyWhenFaster extends SpeedupCriteria
  case object AlwaysEvenIfSlower extends SpeedupCriteria
}

sealed trait OptimizationStrategy {
  def name: String
  def safety: SafetyCriteria
  def speedup: SpeedupCriteria
  def fullName = getClass.getPackage.getName + ".strategy." + name
}

private[streams] abstract class OptimizationStrategyImpl(
    override val name: String,
    override val safety: SafetyCriteria,
    override val speedup: SpeedupCriteria)
  extends OptimizationStrategy

/**
 * Example:
 *   import scalaxy.streams.strategy.aggressive
 *   for (x <- Array(1, 2, 3); y = x * 10; z = y + 2) print(z)
 */
object strategy {
  implicit case object none extends OptimizationStrategyImpl(
    name = "none",
    safety = SafetyCriteria.Safe,
    speedup = SpeedupCriteria.Never)

  /** Performs optimizations that don't alter any Scala semantics, using strict
   * side-effect detection. */
  implicit case object safer extends OptimizationStrategyImpl(
    name = "safer",
    safety = SafetyCriteria.Safe,
    speedup = SpeedupCriteria.OnlyWhenFaster)

  /** Performs optimizations that don't alter any Scala semantics, using reasonably
   * optimistic side-effect detection (for instance, assumes hashCode / equals / toString
   * are side-effect-free for all objects). */
  implicit case object safe extends OptimizationStrategyImpl(
    name = "safe",
    safety = SafetyCriteria.ProbablySafe,
    speedup = SpeedupCriteria.OnlyWhenFaster)

  /** Performs optimizations that don't alter any Scala semantics, using reasonably
   * optimistic side-effect detection (for instance, assumes hashCode / equals / toString
   * are side-effect-free for all objects), but performing rewrites that could be
   * counter-productive / slower.
   *
   * Makes sure all possible lambdas are rewritten away. */
  implicit case object zealous extends OptimizationStrategyImpl(
    name = "zealous",
    safety = SafetyCriteria.ProbablySafe,
    speedup = SpeedupCriteria.AlwaysEvenIfSlower)

  /** Performs unsafe rewrites, ignoring side-effect analysis (which may
   * alter the semantics of the code, but only performing rewrites that are known to
   * be faster. */
  implicit case object aggressive extends OptimizationStrategyImpl(
    name = "aggressive",
    safety = SafetyCriteria.Unsafe,
    speedup = SpeedupCriteria.OnlyWhenFaster)

  /** Performs all possible rewrites, even those known to be slower or unsafe. */
  implicit case object foolish extends OptimizationStrategyImpl(
    name = "foolish",
    safety = SafetyCriteria.Unsafe,
    speedup = SpeedupCriteria.AlwaysEvenIfSlower)

  implicit val default: OptimizationStrategy = safe

  private[this] val strategies =
    List(none, safe, safer, aggressive, foolish)

  private[this] val strategyByName: Map[String, OptimizationStrategy] =
    strategies.map(s => (s.name, s)).toMap

  def forName(name: String): Option[OptimizationStrategy] =
    if (name == null || name == "") None
    else Some(strategyByName(name))

  private[streams] lazy val global: OptimizationStrategy =
    javaProp.orElse(envVarOpt).
      flatMap(forName).
      getOrElse(default)

  private[this] val STRATEGY_PROPERTY = "scalaxy.streams.strategy"
  private[this] val STRATEGY_ENV_VAR = "SCALAXY_STREAMS_STRATEGY"

  private[this] val envVarOpt =
    Option(System.getenv(STRATEGY_ENV_VAR))
  private[this] def javaProp =
    Option(System.getProperty(STRATEGY_PROPERTY))
}

