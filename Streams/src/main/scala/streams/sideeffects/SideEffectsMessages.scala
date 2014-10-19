package scalaxy.streams
import scala.reflect.NameTransformer

private[streams] trait SideEffectsMessages
{
  val global: scala.reflect.api.Universe
  import global._

  implicit class ExtractibleMap[A, B](m: Map[A, B]) {
    def unapply(key: A): Option[B] = {
      val msg = m.get(key)
      // println(s"msg for $key: $msg (known keys: ${m.keys})")
      msg
    }
  }

  def termNamesMessages(m: Map[String, String]): ExtractibleMap[TermName, String] =
    m.map({ case (k, v) => TermName(NameTransformer.encode(k)) -> v })

  private[this] val assumedSideEffectFreeMessageSuffix = "generally assumed to be side-effect free"

  private[this] def anyMethodMessage(name: String) =
    s"Any.$name is $assumedSideEffectFreeMessageSuffix"

  lazy val ProbablySafeNullaryNames = termNamesMessages(Map(
    "hashCode" -> anyMethodMessage("hashCode"),
    "toString" -> anyMethodMessage("toString")
  ))
  private[this] val aritMessage = s"Arithmetic / ensemblist operators are $assumedSideEffectFreeMessageSuffix"

  lazy val ProbablySafeUnaryNames = termNamesMessages(Map(
    "+" -> aritMessage,
    "-" -> aritMessage,
    "/" -> aritMessage,
    "*" -> aritMessage,
    "equals" -> anyMethodMessage("equals"),
    "++" -> s"Collection composition is $assumedSideEffectFreeMessageSuffix",
    "--" -> s"Collection composition is $assumedSideEffectFreeMessageSuffix"
    // "canBuildFrom" -> s"CanBuildFrom's are $assumedSideEffectFreeMessageSuffix",
    // "zipWithIndex" -> s"zipWithIndex is $assumedSideEffectFreeMessageSuffix"
  ))
}
