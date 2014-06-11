package scalaxy.streams

private[streams] trait ClosureStreamOps extends StreamComponents with TransformationClosures
{
  val global: scala.reflect.api.Universe
  import global._

  trait ClosureStreamOp extends StreamOp {
    def param: ValDef
    def body: Tree
    def isMapLike: Boolean = true

    lazy val SomeTransformationClosure(transformationClosure) = q"($param) => $body"

    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) =
      transformationClosure.getPreviousReferencedPaths(paths, isMapLike = isMapLike)
  }
}
