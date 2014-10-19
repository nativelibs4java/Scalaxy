package scalaxy.streams
import scala.reflect.NameTransformer
import scala.collection.breakOut
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer

sealed class SideEffectSeverity(private val level: Int)
    extends Comparable[SideEffectSeverity] {
  override def compareTo(s: SideEffectSeverity) = level.compareTo(s.level)
}

object SideEffectSeverity {
  // case object Safe extends SideEffectSeverity
  /** For instance, toString, equals, hashCode and + are usually considererd probably safe. */
  case object ProbablySafe extends SideEffectSeverity(1)
  // case object ProbablyUnsafe extends SideEffectSeverity(2)
  case object Unsafe extends SideEffectSeverity(3)
}

private[streams] trait SideEffects
{
  val global: scala.reflect.api.Universe
  import global._

  case class SideEffect(tree: Tree, description: String, severity: SideEffectSeverity)

  def analyzeSideEffects(tree: Tree): List[SideEffect]
}
