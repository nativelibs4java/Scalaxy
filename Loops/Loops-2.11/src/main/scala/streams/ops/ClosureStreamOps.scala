package scalaxy.loops

private[loops] trait ClosureStreamOps
    extends Streams
{
  val global: scala.reflect.api.Universe
  import global._

  trait ClosureStreamOp extends StreamOp {
    def param: ValDef
    def body: Tree

    val transformationClosure = {
      val SomeTransformationClosure(tc) = q"($param) => $body"
      tc
    }

    def isMapLike: Boolean = true

    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) =
      transformationClosure.getPreviousReferencedPaths(paths, isMapLike = isMapLike)
  }
}
