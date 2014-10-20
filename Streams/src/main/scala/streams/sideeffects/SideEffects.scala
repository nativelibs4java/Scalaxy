package scalaxy.streams
import scala.reflect.NameTransformer
import scala.collection.breakOut
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer

private[streams] sealed class SideEffectSeverity(
  private val level: Int,
  val description: String)
    extends Comparable[SideEffectSeverity] {
  override def compareTo(s: SideEffectSeverity) = level.compareTo(s.level)
}

/**
 * Severity of a detected side-effect.
 *
 * TODO: rename to Purity / ProbablyPure / Impure to match common naming.
 */
private[streams] object SideEffectSeverity {
  /**
   * For side-effects that are "probably safe".
   *
   * For instance, `toString`, `equals`, `hashCode`, `+`, `++` are considered probably safe.
   * They can be even considered safe when called on truly immutable type such as `Int`,
   * but not on `List[T]`: `List.toString` is only as safe as it's components' `toString`
   * method.
   */
  case object ProbablySafe extends SideEffectSeverity(1, "probably safe")

  /**
   * For side-effects that may have unknown consequences.
   *
   * Most arbitrary references fall into this category (for instance `System.setProperty`).
   */
  case object Unsafe extends SideEffectSeverity(3, "unsafe")
}

private[streams] trait SideEffects
{
  val global: scala.reflect.api.Universe
  import global._

  case class SideEffect(tree: Tree, description: String, severity: SideEffectSeverity)

  def analyzeSideEffects(tree: Tree): List[SideEffect]
}
